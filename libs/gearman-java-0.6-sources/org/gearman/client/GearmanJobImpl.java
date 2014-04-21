/*
 * Copyright (C) 2013 by Eric Lambert <eric.d.lambert@gmail.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.gearman.common.Constants;
import org.gearman.common.GearmanException;
import org.gearman.common.GearmanJobServerSession;
import org.gearman.common.GearmanPacket;
import org.gearman.common.GearmanPacketType;
import org.gearman.common.GearmanServerResponseHandler;
import org.gearman.util.ByteUtils;
import org.gearman.worker.GearmanFunction;
import org.slf4j.LoggerFactory;

public final class GearmanJobImpl implements GearmanJob, GearmanServerResponseHandler {

    static final String DESCRIPTION_PREFIX = "GearmanJob";
    private final String DESCRIPTION;
    private final String functionName;
    private final boolean backgroundJob;
    private final JobPriority priority;
    private final Collection<GearmanIOEventListener> eventListners;
    private byte[] handle = null;
    private byte[] data = new byte[0];
    private String uuid = null;
    private GearmanJobResultImpl jobResult = null;
    private GearmanJobServerSession session = null;
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(
            Constants.GEARMAN_SESSION_LOGGER_NAME);
    private boolean isComplete = false;

    private GearmanJobImpl(String functionName, byte[] data,
            boolean isBackground, JobPriority priority, String uuid)
            throws IllegalArgumentException {
        super();
        if (functionName == null || functionName.trim().equals("")) {
            throw new IllegalArgumentException("Function name can not be " +
                    "null or empty");
        }
        if (data != null) {
            this.data = new byte[data.length];
            System.arraycopy(data, 0, this.data, 0, this.data.length);
        }
        this.functionName = functionName;
        this.backgroundJob = isBackground;
        this.priority = priority;
        this.eventListners = new HashSet<GearmanIOEventListener>();
        if (uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        } else {
            this.uuid = uuid;
        }
        DESCRIPTION = DESCRIPTION_PREFIX + ":" + uuid + ":" + functionName;
    }

    public static GearmanJob createJob(String functionName, byte[] data,
            JobPriority priority, String id) throws IllegalArgumentException {
        GearmanJobImpl job = new GearmanJobImpl(functionName, data, false,
                priority, id);
        return job;
    }

    public static GearmanJob createJob(String functionName, byte[] data, String id)
            throws IllegalArgumentException {
        GearmanJobImpl job = new GearmanJobImpl(functionName, data, false,
                JobPriority.NORMAL, id);
        return job;
    }

    public static GearmanJob createBackgroundJob(String functionName,
            byte[] data, JobPriority priority, String id)
            throws IllegalArgumentException {
        GearmanJobImpl job = new GearmanJobImpl(functionName, data, true,
                priority, id);
        return job;
    }

    public static GearmanJob createBackgroundJob(String functionName,
            byte[] data, String id)
            throws IllegalArgumentException {
        GearmanJobImpl job = new GearmanJobImpl(functionName, data, true,
                JobPriority.NORMAL, id);
        return job;
    }

    public byte[] getHandle() {
        byte [] retHandle = new byte[handle.length];
        System.arraycopy(handle, 0, retHandle, 0, retHandle.length);
        return retHandle;
    }

    public byte[] getID() {
        return uuid.getBytes();
    }

    public byte[] getData() {
        byte[] rt = null;
        if (data == null) {
            rt = new byte[0];
        } else {
            rt = new byte[data.length];
            System.arraycopy(data, 0, rt, 0, rt.length);
        }
        return rt;
    }

    public String getFunctionName() {
        return functionName;
    }

    public boolean isBackgroundJob() {
        return backgroundJob;
    }

    public JobPriority getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return DESCRIPTION;
    }

    public boolean isCancelled() {
        return false;
    }

    public boolean isDone() {
        return isComplete;
    }

    public void setJobServerSession(GearmanJobServerSession sess) {
        if (sess == null) {
            throw new IllegalArgumentException("Job Server connection can " +
                    "not be null");
        }

        LOG.info("Connection for job " + this + " has been set to " +
                sess);
        session = sess;
    }

    public void registerFunction(Callable<GearmanJobResult> function) {
        if (function instanceof GearmanFunction) {
            GearmanFunction gf = (GearmanFunction) function;
            gf.setData(this.data);
        }
    }

    public void registerEventListener(GearmanIOEventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener must not be null");
        }
        eventListners.add(listener);
    }

    public boolean removeEventListener(GearmanIOEventListener listener) {
        return eventListners.remove(listener);
    }

    public GearmanJobResult call() {
        //TODO
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public GearmanJobResult get()
            throws InterruptedException, ExecutionException {
        try {
            return get(-1, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            //This should never happen as a timeout of -1 should never result in a timeout
            throw new ExecutionException(te);
        }
    }

    public GearmanJobResult get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (isComplete) {
            return jobResult.copy();
        }
        int retries = 10;
        long timeOutInMills = timeout < 0 ? -1 : 
            TimeUnit.MILLISECONDS.convert(timeout, unit) +
            System.currentTimeMillis();
        while (!isComplete && !hasTimedOut(timeOutInMills)) {
            try {
                session.driveSessionIO();
            } catch (IOException ioe) {
                retries--;
                LOG.warn("Encountered exception while driving client " +
                        "IO , " + retries + " retries left. [ job = " +
                        this + " exception = " + ioe + "]");
                if (retries == 0) {
                    isComplete = true;
                    LOG.warn("Failed to drive client IO while getting" +
                            " results for job. [ job = " + this +
                            " exception = " + ioe + "]");
                    throw new ExecutionException(ioe);
                }

            }
            Thread.sleep(100);
        }
        if (!isComplete) {
            throw new TimeoutException("Failed to retrieve job result in" +
                    " alloted time (" + timeout + " " + unit.toString() + ").");
        }
        return jobResult.copy();

    }

    public void handleEvent(GearmanPacket event) throws GearmanException {
        GearmanPacketType pt = event.getPacketType();

        switch (pt) {

            case JOB_CREATED:
                LOG.info("job " + this +                            //NOPMD
                        " has received a job created event");
                if (handle != null) {
                    throw new GearmanException("While handling job_create " +
                            "for job with handle" +
                            ByteUtils.fromUTF8Bytes(handle) + " noticed that " +
                            "job object already has a handle " +
                            ByteUtils.fromUTF8Bytes(handle) +
                            ". Duplicate create?");
                }
                handle = event.getDataComponentValue(
                        GearmanPacket.DataComponentName.JOB_HANDLE);
                if (isBackgroundJob()) {
                    isComplete = true;
                    jobResult = new GearmanJobResultImpl(handle, true, null,
                            null, null, -1, -1);
                } else {
                    jobResult = new GearmanJobResultImpl(handle);
                }
                break;

            case WORK_STATUS:
                LOG.info("job " + this + " has received a work " +   //NOPMD
                        "status event");
                validateJobHandle(event.getDataComponentValue(
                        GearmanPacket.DataComponentName.JOB_HANDLE));
                //The legal values of Numerator and Denominator are a bit unclear, it is
                //possible for a job to send back non-numerical values, for now we
                //swallow this since the JobResult and JobStatus classes expect these
                //values to be longs
                long num = 0;
                long den = 0;
                try {
                    num = Long.parseLong(ByteUtils.fromAsciiBytes(
                        event.getDataComponentValue(
                        GearmanPacket.DataComponentName.NUMERATOR)));
                } catch (NumberFormatException nfe) {
                    LOG.info("numerator for job " + this + " has non-numeric value ");
                }
                try {
                    den = Long.parseLong(ByteUtils.fromAsciiBytes(
                        event.getDataComponentValue(
                        GearmanPacket.DataComponentName.DENOMINATOR)));
                } catch (NumberFormatException nfe) {
                    LOG.info("denominator for job " + this + " has non-numeric value ");
                }
                jobResult = jobResult.addJobResult(
                        new GearmanJobResultImpl(handle, false, null, null, null,
                        num, den));
                break;

            case WORK_DATA:
                LOG.info("job " + this + " has received a work " +
                        "data event");
                validateJobHandle(event.getDataComponentValue(
                        GearmanPacket.DataComponentName.JOB_HANDLE));
                jobResult = jobResult.addResults(event.getDataComponentValue(
                        GearmanPacket.DataComponentName.DATA));
                break;

            case WORK_WARNING:
                LOG.info("job " + this + " has received a work " +
                        "warning event");
                validateJobHandle(event.getDataComponentValue(
                        GearmanPacket.DataComponentName.JOB_HANDLE));
                jobResult = jobResult.addWarnings(event.getDataComponentValue(
                        GearmanPacket.DataComponentName.DATA));
                break;

            case WORK_COMPLETE:
                LOG.info("job " + this + " has received a work " +
                        "complete event");
                validateJobHandle(event.getDataComponentValue(
                        GearmanPacket.DataComponentName.JOB_HANDLE));
                jobResult = jobResult.addJobResult(
                        new GearmanJobResultImpl(handle, true,
                        event.getDataComponentValue(
                        GearmanPacket.DataComponentName.DATA),
                        null, null, -1, -1));
                isComplete = true;
                break;

            case WORK_FAIL:
                LOG.info("job " + this + " has received a work " +
                        "fail event");
                validateJobHandle(event.getDataComponentValue(
                        GearmanPacket.DataComponentName.JOB_HANDLE));
                jobResult = jobResult.addJobResult(
                        new GearmanJobResultImpl(handle, false, null, null, null,
                        -1, -1));
                isComplete = true;
                break;

            case WORK_EXCEPTION:
                LOG.info("job " + this + " has received a work " +
                        "exception event");
                validateJobHandle(event.getDataComponentValue(
                        GearmanPacket.DataComponentName.JOB_HANDLE));
                jobResult = jobResult.addExceptions(event.getDataComponentValue(
                        GearmanPacket.DataComponentName.DATA));
                isComplete = true;
                break;

            default:
                LOG.warn("job " + this + " has received an " +
                        "unknown event: " + pt);
                throw new GearmanException("Unknown packet type " + pt);
        }
        for (GearmanIOEventListener listener : eventListners) {
            listener.handleGearmanIOEvent(event);
        }

    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException("Cancel operation not " +
                "supported for GearmanJobs");
    }

    GearmanJobServerSession getSession() {
        return session;
    }

    private void validateJobHandle(byte[] rcvdHandle) throws GearmanException {
        if (!Arrays.equals(handle, rcvdHandle)) {
            throw new GearmanException("Job handle mis-match");
        }
    }

    private boolean hasTimedOut(long millis) {
        return millis < 0 ? false : System.currentTimeMillis() > millis;
    }
}
