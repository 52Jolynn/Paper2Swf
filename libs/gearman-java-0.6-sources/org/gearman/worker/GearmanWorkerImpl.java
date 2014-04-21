/*
 * Copyright (C) 2013 by Eric Lambert <eric.d.lambert@gmail.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman.worker;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.gearman.common.Constants;
import org.gearman.common.GearmanException;
import org.gearman.common.GearmanJobServerConnection;
import org.gearman.common.GearmanJobServerIpConnectionFactory;
import org.gearman.common.GearmanJobServerSession;
import org.gearman.common.GearmanNIOJobServerConnectionFactory;
import org.gearman.common.GearmanPacket;
import org.gearman.common.GearmanPacket.DataComponentName;
import org.gearman.common.GearmanPacketImpl;
import org.gearman.common.GearmanPacketMagic;
import org.gearman.common.GearmanPacketType;
import org.gearman.common.GearmanServerResponseHandler;
import org.gearman.common.GearmanSessionEvent;
import org.gearman.common.GearmanSessionEventHandler;
import org.gearman.common.GearmanTask;

import org.gearman.util.ByteUtils;
import org.slf4j.LoggerFactory;

public class GearmanWorkerImpl
        implements GearmanWorker, GearmanSessionEventHandler {

    static public enum State {

        IDLE, RUNNING, SHUTTINGDOWN
    }
    private static final String DESCRIPION_PREFIX = "GearmanWorker";
    private Queue<GearmanFunction> functionList = null;
    private Selector ioAvailable = null;
    private static final org.slf4j.Logger LOG =  LoggerFactory.getLogger(
            Constants.GEARMAN_WORKER_LOGGER_NAME);
    private String id;
    private Map<String, FunctionDefinition> functionMap;
    private State state;
    private ExecutorService executorService;
    private Map<GearmanJobServerSession, GearmanTask> taskMap = null;
    private Map<SelectionKey,GearmanJobServerSession> sessionMap = null;
    private final GearmanJobServerIpConnectionFactory connFactory = new GearmanNIOJobServerConnectionFactory();
    private volatile boolean jobUniqueIdRequired = false;

    class GrabJobEventHandler implements GearmanServerResponseHandler {

        private final GearmanJobServerSession session;
        private boolean isDone = false;

        GrabJobEventHandler(GearmanJobServerSession session) {
            super();
            this.session = session;
        }

        public void handleEvent(GearmanPacket event) throws GearmanException {
            handleSessionEvent(new GearmanSessionEvent(event, session));
            isDone = true;
        }

        public boolean isDone() {
            return isDone;
        }
    }

    static class FunctionDefinition {

        private final long timeout;
        private final GearmanFunctionFactory factory;

        FunctionDefinition(long timeout, GearmanFunctionFactory factory) {
            this.timeout = timeout;
            this.factory = factory;
        }

        long getTimeout() {
            return timeout;
        }

        GearmanFunctionFactory getFactory() {
            return factory;
        }
    }

    public GearmanWorkerImpl() {
        this (null);
    }

    public GearmanWorkerImpl(ExecutorService executorService) {
        functionList = new LinkedList<GearmanFunction>();
        id = DESCRIPION_PREFIX + ":" + Thread.currentThread().getId();
        functionMap = new HashMap<String, FunctionDefinition>();
        state = State.IDLE;
        this.executorService = executorService;
        taskMap = new HashMap<GearmanJobServerSession, GearmanTask>();
        sessionMap = new ConcurrentHashMap<SelectionKey, GearmanJobServerSession>();
    }

    @Override
    public String toString() {
        return id;
    }

    public void work() {
        if (!state.equals(State.IDLE)) {
            throw new IllegalStateException("Can not call work while worker " +
                    "is running or shutting down");
        }

        state = State.RUNNING;
        // a map keeping track of sessions with connection errors 
        // (to avoid printing an error about them in every reconnect attempt)
        Map<GearmanJobServerSession, Boolean> havingConnectionError = new HashMap<GearmanJobServerSession, Boolean>();
        
        while (isRunning()) {

            // look for sessions which have been disconnected and attempt to reconnect.
            for (Iterator<GearmanJobServerSession> iter = sessionMap.values().iterator(); iter.hasNext();) {
                GearmanJobServerSession sess = iter.next();
                if (!sess.isInitialized()) {
                    try {

                        // reconnect, unregister old selection key and register new one
                        SelectionKey oldKey = sess.isInitialized() ? sess.getSelectionKey() : null;
                        sess.initSession(ioAvailable, this);
                        if (oldKey != null) {
                            iter.remove();
                        }
                        sessionMap.put(sess.getSelectionKey(), sess);

                        // register all functions with the newly reconnected server
                        for (FunctionDefinition d : functionMap.values()) {
                            GearmanTask gsr = new GearmanTask(null, generateCanDoPacket(d));
                            sess.submitTask(gsr);
                        }
                        GearmanTask sessTask = new GearmanTask(
                                new GrabJobEventHandler(sess),
                                new GearmanPacketImpl(GearmanPacketMagic.REQ,
                                getGrabJobPacketType(), new byte[0]));
                        sess.submitTask(sessTask);
                        sess.driveSessionIO();

                        // log reconnection message
                        if (havingConnectionError.get(sess)) {
                            LOG.info("Re-established connection to " + sess.getConnection().toString());
                        }
                        havingConnectionError.put(sess, false);
                    } catch (IOException e) {
                        if (!havingConnectionError.get(sess)) {
                            LOG.warn("Error connecting to " + sess + ", will keep trying..");
                        }

                        havingConnectionError.put(sess, true);

                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e1) {
                        }
                    }
                } else {
                    havingConnectionError.put(sess, false);
                }
            }
	    
            for (GearmanJobServerSession sess : sessionMap.values()) {
            	// if still disconnected, skip
            	if (!sess.isInitialized()) {
            		continue;
            	}
                int interestOps = SelectionKey.OP_READ;
                if (sess.sessionHasDataToWrite()) {
                    interestOps |= SelectionKey.OP_WRITE;
                }
                sess.getSelectionKey().interestOps(interestOps);
            }
            try {
                ioAvailable.select(1);
            } catch (IOException io) {
                LOG.warn("Receieved IOException while" +
                        " selecting for IO",io);
            }

            for (SelectionKey key : ioAvailable.selectedKeys()) {
                 GearmanJobServerSession sess = sessionMap.get(key);
                 if (sess == null) {
                     LOG.warn("Worker does not have " +
                             "session for key " + key);
                     continue;
                 }
                if (!sess.isInitialized()) {
                    continue;
                }
                try {
                    GearmanTask sessTask = taskMap.get(sess);
                    if (sessTask == null) {
                        sessTask = new GearmanTask(                             //NOPMD
                                new GrabJobEventHandler(sess),
                                new GearmanPacketImpl(GearmanPacketMagic.REQ,
                                getGrabJobPacketType(), new byte[0]));
                        taskMap.put(sess, sessTask);
                        sess.submitTask(sessTask);
                        LOG.debug("Worker: " + this + " submitted a " +
                                sessTask.getRequestPacket().getPacketType() +
                                " to session: " + sess);
                    }
                    sess.driveSessionIO();
                    //For the time being we will execute the jobs synchronously
                    //in the future, I expect to change this.
                    if (!functionList.isEmpty()) {
                        GearmanFunction fun = functionList.remove();
                        submitFunction(fun);
                    }
                } catch (IOException ioe) {
                    LOG.warn("Received IOException while driving" +
                            " IO on session " + sess, ioe);
                	sess.closeSession();
                    continue;
                }
            }
        }

        shutDownWorker(true);
    }

    public void handleSessionEvent(GearmanSessionEvent event)
            throws IllegalArgumentException, IllegalStateException {
        GearmanPacket p = event.getPacket();
        GearmanJobServerSession s = event.getSession();
        GearmanPacketType t = p.getPacketType();
        LOG.debug("Worker " + this + " handling session event" +
                " ( Session = " + s + " Event = " + t + " )");
        switch (t) {
            case JOB_ASSIGN:
            	//TODO Figure out what the right behavior is if JobUUIDRequired was false when we submitted but is now true
                taskMap.remove(s);
                addNewJob(event);
                break;
            case JOB_ASSIGN_UNIQ:
            	//TODO Figure out what the right behavior is if JobUUIDRequired was true when we submitted but is now false
                taskMap.remove(s);
                addNewJob(event);
                break;
            case NOOP:
                taskMap.remove(s);
                break;
            case NO_JOB:
                GearmanTask preSleepTask = new GearmanTask(new GrabJobEventHandler(s),
                        new GearmanPacketImpl(GearmanPacketMagic.REQ,
                        GearmanPacketType.PRE_SLEEP, new byte[0]));
                taskMap.put(s, preSleepTask);
                s.submitTask(preSleepTask);
                break;
            case ECHO_RES:
                break;
            case OPTION_RES:
                break;
            case ERROR:
                s.closeSession();
                break;
            default:
                LOG.warn("Received unknown packet type " + t +
                        " from session " + s + ". Closing connection.");
                s.closeSession();
        }
    }
    
    public boolean addServer(String host, int port) {
    	return addServer(connFactory.createConnection(host, port));
    }

    public boolean addServer(GearmanJobServerConnection conn)
            throws IllegalArgumentException, IllegalStateException {

        if (conn == null) {
            throw new IllegalArgumentException("Connection can not be null");
        }
        //this is a sub-optimal way to look for dups, but addJobServer
        //ops should be infrequent enough that this should be a big penalty
        for (GearmanJobServerSession sess : sessionMap.values()) {
            if (sess.getConnection().equals(conn)) {
                return true;
            }
        }

        GearmanJobServerSession session =
                new GearmanJobServerSession(conn);
        if (ioAvailable == null) {
            try {
                ioAvailable = Selector.open();
            } catch (IOException ioe) {
                LOG.warn("Failed to connect to job server "
                    + conn + ".",ioe);
                return false;
            }
        }
        try {
            session.initSession(ioAvailable, this);
        } catch (IOException ioe) {
            LOG.warn("Failed to initialize session with job server "
                    + conn + ".",ioe);
            return false;
        }
        SelectionKey key = session.getSelectionKey();
        if (key == null) {
            String msg = "Session " + session + " has a null " +
                    "selection key. Server will not be added to worker.";
            LOG.warn(msg);
            throw new IllegalStateException(msg);
        }
        sessionMap.put(key, session);

        GearmanPacket p = new GearmanPacketImpl(GearmanPacketMagic.REQ,
                GearmanPacketType.SET_CLIENT_ID, ByteUtils.toUTF8Bytes(id));
        session.submitTask(new GearmanTask(p));

        for (FunctionDefinition def : functionMap.values()) {
            p = generateCanDoPacket(def);
            session.submitTask(new GearmanTask(p));                              //NOPMD
        }

        p = new GearmanPacketImpl(GearmanPacketMagic.REQ,
                getGrabJobPacketType(), new byte[0]);
        GearmanTask gsr = new GearmanTask(
                new GrabJobEventHandler(session), p);
        taskMap.put(session, gsr);
        session.submitTask(gsr);
        
        LOG.debug("Added server " + conn + " to worker " + this);
        return true;
    }

    public boolean hasServer(GearmanJobServerConnection conn) {
        boolean foundIt = false;
        for (GearmanJobServerSession sess : sessionMap.values()) {
            if (sess.getConnection().equals(conn)) {
                foundIt = true;
            }
        }
        return foundIt;
    }

    public String echo(String text, GearmanJobServerConnection conn) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void registerFunction(String function, long timeout) {
        registerFunctionFactory(new DefaultGearmanFunctionFactory(function),
                timeout);
    }

    public void registerFunction(String function) {
        registerFunction(function, 0);
    }

    public void registerFunction(Class<? extends GearmanFunction> function) {
        registerFunction(function, 0);
    }

    public void registerFunction(Class<? extends GearmanFunction> function,
            long timeout) {
        registerFunctionFactory(new DefaultGearmanFunctionFactory(
                function.getName()), timeout);
    }

    public void registerFunctionFactory(GearmanFunctionFactory factory) {
        registerFunctionFactory(factory, 0);
    }

    public void registerFunctionFactory(GearmanFunctionFactory factory,
            long timeout) {
        if (functionMap.containsKey(factory.getFunctionName())) {
            return;
        }
        FunctionDefinition def = new FunctionDefinition(timeout, factory);
        functionMap.put(factory.getFunctionName(), def);
        sendToAll(generateCanDoPacket(def));
        LOG.debug("Worker " + this + " has registered function " +
                factory.getFunctionName());
    }
    
    public Set<String> getRegisteredFunctions() {
        HashSet<String> functions = new HashSet<String>();
        for (FunctionDefinition def : functionMap.values()) {
            functions.add(def.factory.getFunctionName());
        }
        return functions;
    }

    public void setWorkerID(String id) throws IllegalArgumentException {
        if (id == null) {
            throw new IllegalArgumentException("Worker ID may not be null");
        }
        this.id = id;
        sendToAll(new GearmanPacketImpl(GearmanPacketMagic.REQ,
                GearmanPacketType.SET_CLIENT_ID, ByteUtils.toUTF8Bytes(id)));
    }

    public void setWorkerID(String id, GearmanJobServerConnection conn) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getWorkerID() {
        return id;
    }

    public String getWorkerID(GearmanJobServerConnection conn) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void unregisterFunction(String functionName) {
        functionMap.remove(functionName);
        sendToAll(new GearmanPacketImpl(GearmanPacketMagic.REQ,
                GearmanPacketType.CANT_DO, ByteUtils.toUTF8Bytes(functionName)));
        LOG.debug("Worker " + this + " has unregistered function " +
                functionName);
    }

    public void unregisterAll() {
        functionMap.clear();
        sendToAll(new GearmanPacketImpl(GearmanPacketMagic.REQ,
                GearmanPacketType.RESET_ABILITIES, new byte[0]));
    }

    public void stop() {
        state = State.SHUTTINGDOWN;
    }

    public List<Exception> shutdown() {
        return shutDownWorker(false);
    }

    public boolean isRunning() {
        return state.equals(State.RUNNING);
    }

	public void setJobUniqueIdRequired(boolean requiresJobUUID) {
		jobUniqueIdRequired = requiresJobUUID;
	}

	public boolean isJobUniqueIdRequired() {
		return jobUniqueIdRequired;
	}

    private GearmanPacket generateCanDoPacket(FunctionDefinition def) {
        GearmanPacketType pt = GearmanPacketType.CAN_DO;
        byte[] data = null;
        byte[] name = ByteUtils.toUTF8Bytes(def.getFactory().getFunctionName());
        long timeout = def.getTimeout();

        if (timeout > 0) {
            pt = GearmanPacketType.CAN_DO_TIMEOUT;
            byte[] to = ByteUtils.toUTF8Bytes(String.valueOf(timeout));
            data = new byte[name.length + to.length + 1];
            System.arraycopy(name, 0, data, 0, name.length);
            data[name.length] = ByteUtils.NULL;
            System.arraycopy(to, 0, data, name.length + 1, to.length);
        } else {
            data = name;
        }
        return new GearmanPacketImpl(GearmanPacketMagic.REQ, pt, data);
    }

    private void sendToAll(GearmanPacket p) {
        sendToAll(null, p);
    }

    private void sendToAll(GearmanServerResponseHandler handler, GearmanPacket p) {
        GearmanTask gsr = new GearmanTask(handler, p);
        for (GearmanJobServerSession sess : sessionMap.values()) {
            sess.submitTask(gsr);
        }
    }

    /*
     * For the time being this will always return an empty list of
     * exceptions because closeSession does not throw an exception
     */
    private List<Exception> shutDownWorker(boolean completeTasks) {
        LOG.info("Commencing shutdowm of worker " + this);

        ArrayList<Exception> exceptions = new ArrayList<Exception>();

        // This gives any jobs in flight a chance to complete
        if (executorService != null) {
            if (completeTasks) {
                executorService.shutdown();
            } else {
                executorService.shutdownNow();
            }
        }

        for (GearmanJobServerSession sess : sessionMap.values()) {
            sess.closeSession();
        }
        try {
            ioAvailable.close();
        } catch (IOException ioe) {
            LOG.warn("Encountered IOException while closing selector for worker: ", ioe);
        }
        state = State.IDLE;
        LOG.info("Completed shutdowm of worker " + this);

        return exceptions;
    }

    private void addNewJob(GearmanSessionEvent event) {
        byte[] handle, data, functionNameBytes, unique;
        GearmanPacket p = event.getPacket();
        GearmanJobServerSession sess = event.getSession();
        String functionName;
        handle = p.getDataComponentValue(
                GearmanPacket.DataComponentName.JOB_HANDLE);
        functionNameBytes = p.getDataComponentValue(
                GearmanPacket.DataComponentName.FUNCTION_NAME);
        data = p.getDataComponentValue(
                GearmanPacket.DataComponentName.DATA);
        unique = p.getDataComponentValue(DataComponentName.UNIQUE_ID);
        functionName = ByteUtils.fromUTF8Bytes(functionNameBytes);
        FunctionDefinition def = functionMap.get(functionName);
        if (def == null) {
            GearmanTask gsr = new GearmanTask(
                    new GearmanPacketImpl(GearmanPacketMagic.REQ,
                    GearmanPacketType.WORK_FAIL, handle));
            sess.submitTask(gsr);
        } else {
            GearmanFunction function = def.getFactory().getFunction();
            function.setData(data);
            function.setJobHandle(handle);
            function.registerEventListener(sess);
            if (unique != null && unique.length > 0) {
            	function.setUniqueId(unique);
            }
            functionList.add(function);
        }
    }

    private void submitFunction(GearmanFunction fun) {
        try {
            if (executorService == null) {
                fun.call();
            } else {
                executorService.submit(fun);
            }
        } catch (Exception e) {
            LOG.warn("Exception while executing function " + fun.getName(), e);
        }
    }

    private GearmanPacketType getGrabJobPacketType() {
        if (jobUniqueIdRequired) {
            return GearmanPacketType.GRAB_JOB_UNIQ;
        }
        return GearmanPacketType.GRAB_JOB;
    }
}
