/*
 * Copyright (C) 2013 by Eric Lambert <eric.d.lambert@gmail.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman.client;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.gearman.common.Constants;
import org.gearman.common.GearmanException;
import org.gearman.common.GearmanJobServerConnection;
import org.gearman.common.GearmanJobServerIpConnectionFactory;
import org.gearman.common.GearmanJobServerSession;
import org.gearman.common.GearmanNIOJobServerConnection;
import org.gearman.common.GearmanNIOJobServerConnectionFactory;
import org.gearman.common.GearmanPacket;
import org.gearman.common.GearmanPacketImpl;
import org.gearman.common.GearmanPacketMagic;
import org.gearman.common.GearmanPacketType;
import org.gearman.common.GearmanServerResponseHandler;
import org.gearman.common.GearmanSessionEvent;
import org.gearman.common.GearmanSessionEventHandler;
import org.gearman.common.GearmanTask;
import org.gearman.util.ByteUtils;

//TODO change the server selection to use open connection
/* TODO
 * -documentation (specifically the unsupported methods)
 * -handle dropped sessions
 * -have updateJobStatus handle any type of job (currently only handles jobimpl)
 */

/*
 * ISSUES/RFEs
 * -several of the invoke/submit/execute methods throw OpNotSupported exception
 *  because they rely task cancelation, which we dont support
 * -shutdownNow will always return an empty set, there is a few problems here
 *  1) a gearmanJob is a callable, not a runnable
 *  2) we track requests, not jobs inside the session object
 * -selectUpdate should provide the ability to on select on certain events
 */
public class GearmanClientImpl
        implements GearmanClient, GearmanSessionEventHandler {

    private static enum state {

        RUNNING, SHUTTINGDOWN, TERMINATED
    }

    private static final String DESCRIPION_PREFIX = "GearmanClient";
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(
            Constants.GEARMAN_CLIENT_LOGGER_NAME);
    private static final String CLIENT_NOT_ACTIVE = "Client is not active";
    private static final long DEFAULT_DRIVE_REQUEST_TIMEOUT = 2000;
    private static final byte [] EXCEPTIONS = ByteUtils.toAsciiBytes("exceptions"); 
    private final String DESCRIPTION;
    private Map<SelectionKey, GearmanJobServerSession> sessionsMap = null;
    private Selector ioAvailable = null;
    private state runState = state.RUNNING;
    private final Map<GearmanJobServerSession, Map<JobHandle, GearmanJobImpl>> sessionJobsMap;
    private GearmanJobImpl jobAwatingCreation;
    private final Timer timer = new Timer();
    private final GearmanJobServerIpConnectionFactory connFactory = new GearmanNIOJobServerConnectionFactory();
    private final long driveRequestTimeout;

    private static class Alarm extends TimerTask {

        private final AtomicBoolean timesUp = new AtomicBoolean(false);

        @Override
        public void run() {
            timesUp.set(true);
        }

        public boolean hasFired() {
            return timesUp.get();
        }
    }

    private static class JobHandle {

        private final byte[] jobHandle;

        JobHandle(GearmanJobImpl job) {
            this(job.getHandle());
        }

        JobHandle(byte[] handle) {
            jobHandle = new byte[handle.length];
            System.arraycopy(handle, 0, jobHandle, 0, handle.length);
        }

        @Override
        public boolean equals(Object that) {
            if (that == null) {
                return false;
            }
            if (!(that instanceof JobHandle)) {
                return false;
            }
            JobHandle thatHandle = (JobHandle) that;
            return Arrays.equals(jobHandle, thatHandle.jobHandle);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(jobHandle);
        }
    }

    /**
     * Create a new GearmanClient instance. Instances are not thread-safe and
     * should not be shared across threads.
     */
    public GearmanClientImpl() {
        this(DEFAULT_DRIVE_REQUEST_TIMEOUT);
    }
    
    public GearmanClientImpl(long driveRequestTimeout) {
    	if (driveRequestTimeout < 0) {
    		throw new IllegalArgumentException("Drive request timeout must be 0" +
    				" or greater.");
    	}
    	sessionsMap = new Hashtable<SelectionKey, GearmanJobServerSession>();
        sessionJobsMap = new HashMap<GearmanJobServerSession, Map<JobHandle, GearmanJobImpl>>();
        DESCRIPTION = DESCRIPION_PREFIX + ":" + Thread.currentThread().getId();
        this.driveRequestTimeout = driveRequestTimeout;
    }

    public boolean addJobServer(String host, int port) {
	return addJobServer(host, port, true);
    }

    // TODO move to interface?
    public boolean addJobServer(String host, int port,
	    boolean serverForwardsExceptions) {
	return addJobServer(connFactory.createConnection(host, port),
		serverForwardsExceptions);
    }

    public boolean addJobServer(GearmanJobServerConnection newconn)
	    throws IllegalArgumentException, IllegalStateException {
	return addJobServer(newconn, true);
    }

    public boolean addJobServer(GearmanJobServerConnection newconn,
	    boolean serverForwardsExceptions) throws IllegalArgumentException,
	    IllegalStateException {

        //TODO remove this restriction
        if (!(newconn instanceof GearmanNIOJobServerConnection)) {
            throw new IllegalArgumentException("Client currently only " +
                    "supports " +
                    GearmanNIOJobServerConnection.class.getName() +
                    " connections.");
        }

        GearmanNIOJobServerConnection conn =
                (GearmanNIOJobServerConnection) newconn;

        if (!runState.equals(state.RUNNING)) {
            throw new RejectedExecutionException("Client has been shutdown");
        }

        GearmanJobServerSession session = new GearmanJobServerSession(conn);
        if (sessionsMap.values().contains(session)) {
            LOG.debug("The server " + newconn + " was previously " +
                    "added to the client. Ignoring add request.");
            return true;
        }

        try {
            if (ioAvailable == null) {
                ioAvailable = Selector.open();
            }

            session.initSession(ioAvailable, this);
            SelectionKey key = session.getSelectionKey();
            sessionsMap.put(key, session);
        } catch (IOException ioe) {
            LOG.warn("Failed to connect to job server "
                    + newconn + ".",ioe);
            return false;
	}
	if (serverForwardsExceptions && !setForwardExceptions(session)) {
	    return false;
	}
	sessionJobsMap.put(session, new HashMap<JobHandle, GearmanJobImpl>());

        LOG.info("Added connection " + conn + " to client " + this);
        return true;
    }

    public boolean hasConnection(GearmanJobServerConnection conn) {
        for (GearmanJobServerSession sess : sessionsMap.values()) {
            if (sess.getConnection().equals(conn)) {
                return true;
            }
        }
        return false;
    }

    public List<GearmanJobServerConnection> getSetOfJobServers()
            throws IllegalStateException {
        if (!runState.equals(state.RUNNING)) {
            throw new IllegalStateException(CLIENT_NOT_ACTIVE);
        }

        ArrayList<GearmanJobServerConnection> retSet =
                new ArrayList<GearmanJobServerConnection>();
        for (GearmanJobServerSession sess : sessionsMap.values()) {
            retSet.add(sess.getConnection());
        }
        return retSet;
    }

    public void removeJobServer(GearmanJobServerConnection conn)
            throws IllegalArgumentException, IllegalStateException {
        if (!runState.equals(state.RUNNING)) {
            throw new IllegalStateException("JobServers can not be removed " +
                    "once shutdown has been commenced.");
        }

        //TODO, make this better
        Iterator<GearmanJobServerSession> iter = sessionsMap.values().iterator();
        GearmanJobServerSession session = null;
        boolean foundit = false;
        while (iter.hasNext() && !foundit) {
            session = iter.next();
            if (session.getConnection().equals(conn)) {
                foundit = true;
            }
        }

        if (!foundit) {
            throw new IllegalArgumentException("JobServer " + conn + " has not" +
                    " been registered with this client.");
        }

        shutDownSession(session);
        LOG.info("Removed job server " + conn + " from client " +
                this);
    }

    public <T> Future<T> submit(Callable<T> task) {

        if (task == null) {
            throw new IllegalStateException("Null task was submitted to " +
                    "gearman client");
        }

        if (!runState.equals(state.RUNNING)) {
            throw new RejectedExecutionException("Client has been shutdown");
        }

        if (!(task instanceof GearmanServerResponseHandler)) {
            throw new RejectedExecutionException("Task must implement the " +
                    GearmanServerResponseHandler.class + " interface to" +
                    " submitted to this client");
        }

        GearmanJobImpl job = (GearmanJobImpl) task;
        GearmanServerResponseHandler handler = (GearmanServerResponseHandler) job;


        if (job.isDone()) {
            throw new RejectedExecutionException("Task can not be resubmitted ");
        }

        GearmanJobServerSession session = null;
        try {
            session = getSessionForTask();
        } catch (IOException ioe) {
            throw new RejectedExecutionException(ioe);
        }
        job.setJobServerSession(session);
        GearmanPacket submitRequest = getPacketFromJob(job);
        GearmanTask submittedJob =
                new GearmanTask(handler, submitRequest);
        session.submitTask(submittedJob);
        LOG.info("Client " + this + " has submitted job " + job +    //NOPMD
                " to session " + session + ". Job has been added to the " +     //NOPMD
                "active job queue");
        try {
            jobAwatingCreation = job;
            if (!(driveRequestTillState(submittedJob,
                    GearmanTask.State.RUNNING))) {
                throw new RejectedExecutionException("Timed out waiting for" +
                        " submission of " + job + " to complete");
            }
        } catch (IOException ioe) {
            LOG.warn("Client " + this + " encounted an " +
                    "IOException while drivingIO",ioe);
            throw new RejectedExecutionException("Failed to successfully " +
                    "submit" + job + " due to IOException",ioe);
        } finally {
            jobAwatingCreation = null;
        }

        return (Future<T>) job;
    }

    public <T> Future<T> submit(Runnable task, T result) {
        throw new UnsupportedOperationException("Client does not support " +
                "execution of non-GearmanJob objects");
    }

    public Future<?> submit(Runnable task) {
        throw new UnsupportedOperationException("Client does not support " +
                "execution of non-GearmanJob objects");
    }

    public void execute(Runnable command) {
        throw new UnsupportedOperationException("Client does not support " +
                "execution of non-GearmanJob objects");
    }

    // NOTE, there is a subtle difference between the ExecutorService invoke*
    // method signatures in jdk1.5 and jdk1.6 that requires we implement these
    // methods in their 'erasure' format (that is without the use of the
    // specified generic types), otherwise we will not be able to compile this
    // class using both compilers.
    
    @SuppressWarnings("unchecked") //NOPMD
    public List invokeAll(Collection tasks) throws InterruptedException {
        ArrayList<Future> futures = new ArrayList<Future>();
        Iterator<Callable<Future>> iter = tasks.iterator();
        while (iter.hasNext()) {
            Callable<Future> curTask = iter.next();
            futures.add(this.submit(curTask));
        }
        for (Future results : futures) {
            try {
                results.get();
            } catch (ExecutionException ee) {
                LOG.warn("Failed to execute task " +
                        results + ".",ee);
            }
        }
        return futures;
    }

    @SuppressWarnings("unchecked")
    public List invokeAll(Collection tasks,
            long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @SuppressWarnings("unchecked")
    public Future invokeAny(Collection tasks)
            throws InterruptedException, ExecutionException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @SuppressWarnings("unchecked")
    public Future invokeAny(Collection tasks, long timeout,
            TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public GearmanJobStatus getJobStatus(GearmanJob job) throws IOException,
            GearmanException, IllegalStateException {
        // we need to know what session to ask that information can not be
        // gleened from the job object.
        if (!(job instanceof GearmanJobImpl)) {
            throw new IllegalArgumentException("job must be of type " +
                    GearmanJobImpl.class);
        }
        GearmanJobImpl jobImpl = (GearmanJobImpl) job;
        return updateJobStatus(jobImpl.getHandle(), jobImpl.getSession());
    }

    public byte[] echo(byte[] data) throws IOException, GearmanException {
        if (!runState.equals(state.RUNNING)) {
            throw new IllegalStateException(CLIENT_NOT_ACTIVE);
        }
        GearmanPacket echoRequest = new GearmanPacketImpl(GearmanPacketMagic.REQ,
                GearmanPacketType.ECHO_REQ,data);
        GearmanEchoResponseHandler handler = new GearmanEchoResponseHandler();
        GearmanTask t = new GearmanTask(handler, echoRequest);
        GearmanJobServerSession session = getSessionForTask();
        session.submitTask(t);
        LOG.info("Client " + this + " has submitted echo request " +
                "(payload = " + ByteUtils.toHex(data) + " to session " +
                session);
        if (!driveRequestTillState(t, GearmanTask.State.FINISHED)) {
            throw new GearmanException("Failed to execute echo request " + t +
                    " to session " + session);
        }
        LOG.info("Client " + this + " has completed echo request " +
                "to session " + session);
        return handler.getResults();
    }

    public int getNumberofActiveJobs() throws IllegalStateException {
        if (runState.equals(state.TERMINATED)) {
            throw new IllegalStateException(CLIENT_NOT_ACTIVE);
        }
        int size = 0;

        for (Map<JobHandle, GearmanJobImpl> cur : sessionJobsMap.values()) {
           size += cur.size();
        }
        return size;
    }

    public void handleSessionEvent(GearmanSessionEvent event)
            throws IllegalArgumentException, IllegalStateException {
        GearmanPacket p = event.getPacket();
        GearmanJobServerSession s = event.getSession();
        GearmanPacketType t = p.getPacketType();
        Map<JobHandle, GearmanJobImpl> jobsMaps = sessionJobsMap.get(s);

        switch (t) {
            case JOB_CREATED:
                if (jobAwatingCreation == null) {
                    throw new IllegalStateException("Recevied job creation "
                            + "message but have not job awaiting submission.");
                }
                if (!jobAwatingCreation.isBackgroundJob()) {
                    jobsMaps.put(new JobHandle(jobAwatingCreation), jobAwatingCreation);
                }
                break;
            case WORK_DATA:
            case WORK_STATUS:
            case WORK_WARNING:
            case WORK_COMPLETE:
            case WORK_FAIL:
            case WORK_EXCEPTION:
                JobHandle handle = new JobHandle(p.getDataComponentValue(
                        GearmanPacket.DataComponentName.JOB_HANDLE));
                GearmanJobImpl job = jobsMaps.get(handle);
                if (job == null) {
                    LOG.warn("Client received packet from server" +
                            " for unknown job ( job_handle = " + handle +
                            " packet = " + t +" )");
                } else {
                    job.handleEvent(p);
                    if (job.isDone()) {
                        jobsMaps.remove(handle);
                    }
                }
                break;
            case ERROR:
                String errCode = ByteUtils.fromUTF8Bytes(
                        p.getDataComponentValue(
                        GearmanPacket.DataComponentName.ERROR_CODE));
                String errMsg = ByteUtils.fromUTF8Bytes(
                        p.getDataComponentValue(
                        GearmanPacket.DataComponentName.ERROR_TEXT));
                LOG.warn( "Received error code " + errCode +
                        "( " + errMsg + " )" + " from session " + s +
                        ". Shutting session down");
                shutDownSession(s);
                if (sessionsMap.isEmpty()) {
                    shutdown();
                }
                break;
            default:
                LOG.warn( "received un-expected packet from Job" +
                        " Server Session: " + p + ". Shutting down session");
                shutDownSession(s);
                if (sessionsMap.isEmpty()) {
                    shutdown();
                }
        }
    }

    public void shutdown() {
        if (!runState.equals(state.RUNNING)) {
            return;
        }
        runState = state.SHUTTINGDOWN;
        LOG.info("Commencing controlled shutdown of client: " + this);
        try {
            awaitTermination(-1, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            LOG.info("Client shutdown interrupted while waiting" +
                    " for jobs to terminate.");
        }
        shutdownNow();
        LOG.info("Completed ontrolled shutdown of client: " + this);
    }

    public List<Runnable> shutdownNow() {
        runState = state.SHUTTINGDOWN;
        LOG.info("Commencing immediate shutdown of client: " + this);
        timer.cancel();
        Iterator<GearmanJobServerSession> sessions =
                sessionsMap.values().iterator();
        while (sessions.hasNext()) {
            GearmanJobServerSession curSession = sessions.next();
            if (!curSession.isInitialized()) {
                continue;
            }
            try {
                curSession.closeSession();
            } catch (Exception e) {
                LOG.warn( "Failed to closes session " + curSession +
                        " while performing immediate shutdown of client " +
                        this + ". Encountered the following exception " + e);
            }
            sessions.remove();
        }
        sessionsMap.clear();
        sessionsMap = null;
        runState = state.TERMINATED;
        try {
            ioAvailable.close();
        } catch (IOException ioe) {
            LOG.warn( "Encountered IOException while closing selector for client ", ioe);
        }
        LOG.info("Completed shutdown of client: " + this);
        return new ArrayList<Runnable>();
    }

    public boolean isShutdown() {
        return !runState.equals(state.RUNNING);
    }

    public boolean isTerminated() {
        return runState.equals(state.TERMINATED);
    }

    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        TimeUnit sessionUnit = TimeUnit.MILLISECONDS;
        long timeLeft = -1;
        long timeOutInMills = timeout < 0 ? -1 :
            TimeUnit.MILLISECONDS.convert(timeout, unit) +
            System.currentTimeMillis();

        if (getNumberofActiveJobs() == 0) {
            return true;
        }

        for (GearmanJobServerSession curSession : sessionsMap.values()) {
            if (!(curSession.isInitialized())) {
                continue;
            }
            //negative timeout means block till completion
            if (timeout >= 0) {
                timeLeft = timeOutInMills - System.currentTimeMillis();
                if (timeLeft <= 0) {
                    LOG.warn( "awaitTermination exceeded timeout.");
                    break;
                }
            }
            try {
                curSession.waitForTasksToComplete(timeLeft, sessionUnit);
            } catch (TimeoutException te) {
                LOG.info("timed out waiting for all tasks to complete");
                break;
            }
        }
        return getNumberofActiveJobs() == 0;
    }

    @Override
    public String toString() {
        return DESCRIPTION;
    }

    private void driveClientIO() throws IOException, GearmanException {
        for (GearmanJobServerSession sess : sessionsMap.values()) {
            int interestOps = SelectionKey.OP_READ;
            if (sess.sessionHasDataToWrite()) {
                interestOps |= SelectionKey.OP_WRITE;
            }
            try {
            	sess.getSelectionKey().interestOps(interestOps);
            } catch (IllegalStateException ise) {
            	LOG.warn("Unable to drive IO for session " + sess + "," +
            			" skipping.",ise);
            	continue;
            }
        }
        ioAvailable.selectNow();
        Set<SelectionKey> keys = ioAvailable.selectedKeys();
        LOG.trace("Driving IO for client " + this + ". " +
                keys.size() + " session(s) currently available for IO");
        Iterator<SelectionKey> iter = keys.iterator();
        while (iter.hasNext()) {
            SelectionKey key = iter.next();
            GearmanJobServerSession s = sessionsMap.get(key);
            s.driveSessionIO();
        }
    }

    private GearmanJobStatus updateJobStatus(byte[] jobhandle,
            GearmanJobServerSession session) throws IOException,
            IllegalStateException, GearmanException {
        if (!runState.equals(state.RUNNING)) {
            throw new IllegalStateException(CLIENT_NOT_ACTIVE);
        }

        if (jobhandle == null || jobhandle.length == 0) {
            throw new IllegalStateException("Invalid job handle. Handle must" +
                    " not be null nor empty");
        }
        GearmanPacket statusRequest = new GearmanPacketImpl(
                GearmanPacketMagic.REQ, GearmanPacketType.GET_STATUS, jobhandle);
        GearmanServerResponseHandler handler =
                (GearmanServerResponseHandler) new GearmanJobStatusImpl();
        GearmanTask t = new GearmanTask(
                handler, statusRequest);
        session.submitTask(t);
        if (!driveRequestTillState(t,GearmanTask.State.FINISHED)) {
            throw new GearmanException("Failed to execute jobstatus request " +
                    t + " to session " + session);
        }
        return (GearmanJobStatus) handler;
    }

    private GearmanJobServerSession getSessionForTask() throws IOException {
        if (sessionsMap.values().isEmpty()) {
            throw new IOException("No servers registered with client");
        }

        ArrayList<GearmanJobServerSession> sessions = new
                ArrayList<GearmanJobServerSession>();
        sessions.addAll(sessionsMap.values());
        Random rand = new Random(System.currentTimeMillis());
        int s = rand.nextInt(sessions.size());
        GearmanJobServerSession session = sessions.get(s);
        IOException ioe = new IOException("No servers available for client");
        for (int i = 0; i < sessions.size(); i++) {
            if (!session.isInitialized()) {
                try {
                    session.initSession(ioAvailable, this);
                    sessionsMap.put(session.getSelectionKey(), session);
                } catch (Exception e) {
                    if (sessions.size() > 1) {
                        // try next one
                        int prev = s;
                        s = (s + 1) % sessions.size();
                        session = sessions.get(s);
                        LOG.warn("Got exception attempting to retrieve " +
                        		"session for task. Try next server at " +
                        		"pos={}, previous pos={}", s, prev);
                        continue;
                    } else {
                        break;
                    }
                }
            }
            return session;
        }
        throw ioe;
    }

    private boolean driveRequestTillState(GearmanTask r, GearmanTask.State state)
            throws IOException, GearmanException {
        Alarm alarm = new Alarm();
        timer.schedule(alarm, driveRequestTimeout);
        while (r.getState().compareTo(state) < 0 && !(alarm.hasFired())) {
            driveClientIO();
        }
        return r.getState().compareTo(state) >= 0;
    }

    private GearmanPacket getPacketFromJob(GearmanJob job) {
        int destPos = 0;
        GearmanPacketMagic magic = GearmanPacketMagic.REQ;
        GearmanPacketType type = null;
        byte[] packetdata = null;
        byte[] fnname = ByteUtils.toAsciiBytes(job.getFunctionName());
        byte[] uid = job.getID();
        byte[] data = job.getData();
        if (job.getPriority().equals(GearmanJob.JobPriority.HIGH)) {
            type = job.isBackgroundJob() ? GearmanPacketType.SUBMIT_JOB_HIGH_BG :
                GearmanPacketType.SUBMIT_JOB_HIGH;
        }
        if (job.getPriority().equals(GearmanJob.JobPriority.LOW)) {
            type = job.isBackgroundJob() ? GearmanPacketType.SUBMIT_JOB_LOW_BG :
                GearmanPacketType.SUBMIT_JOB_LOW;
        }
        if (job.getPriority().equals(GearmanJob.JobPriority.NORMAL)) {
            type = job.isBackgroundJob() ? GearmanPacketType.SUBMIT_JOB_BG :
                GearmanPacketType.SUBMIT_JOB;
        }
        packetdata = new byte[fnname.length + uid.length + data.length + 2];
        System.arraycopy(fnname, 0, packetdata, destPos, fnname.length);
        destPos += fnname.length;
        packetdata[destPos++] = ByteUtils.NULL;
        System.arraycopy(uid, 0, packetdata, destPos, uid.length);
        destPos += uid.length;
        packetdata[destPos++] = ByteUtils.NULL;
        System.arraycopy(data, 0, packetdata, destPos, data.length);
        return new GearmanPacketImpl(magic, type, packetdata);
    }

    private void shutDownSession(GearmanJobServerSession s) {
        if (s.isInitialized()) {
            SelectionKey k = s.getSelectionKey();
            if (k != null) {
                sessionsMap.remove(k);
                k.cancel();
            }
            s.closeSession();
        }
        sessionJobsMap.remove(s);
    }

    private boolean setForwardExceptions(GearmanJobServerSession session) {
	GearmanServerResponseHandler optionReqHandler = new GearmanServerResponseHandler() {
	    boolean isDone = false;

	    public boolean isDone() {
		return isDone;
	    }

	    public void handleEvent(GearmanPacket event)
		    throws GearmanException {
		GearmanPacketType type = event.getPacketType();
		if (type.equals(GearmanPacketType.OPTION_RES)
			&& Arrays.equals(EXCEPTIONS, event.getData())) {
		    isDone = true;
		}
	    }
	};
	GearmanTask setExceptionsTask = new GearmanTask(optionReqHandler,
		new GearmanPacketImpl(GearmanPacketMagic.REQ,
			GearmanPacketType.OPTION_REQ, EXCEPTIONS));
	session.submitTask(setExceptionsTask);
	Exception e = null;
	boolean success = false;
	try {
	    success = driveRequestTillState(setExceptionsTask,
		    GearmanTask.State.FINISHED);
	} catch (IOException ioe) {
	    e = ioe;
	}
	if (!success) {
	    if (e != null) {
		LOG.info("Failed to set forward-exceptions option to "
			+ session.getConnection(), e);
	    } else {
		LOG.info("Failed to set forward-exceptions option to "
			+ session.getConnection());
	    }
	    return false;
	}
	return true;
    }
}
