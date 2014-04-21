/*
 * Copyright (C) 2009 by Eric Lambert <Eric.Lambert@sun.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman.client;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * The Gearman Job is a basic undivisible unit of work to be executed by a
 * Gearman system. The job is created by the user and submitted to the Gearman
 * Client for execution.
 */
public interface GearmanJob
        extends Callable<GearmanJobResult>, Future<GearmanJobResult> {

    public static enum JobPriority {

        HIGH, NORMAL, LOW
    }

    /**
     * Retrieves the handle used by the Gearman Job Server to which this job
     * was submitted to as a means to identify this job.
     *
     * @return The handle assigned to the job by the Gearman Job Server. If the
     *         job has not been submitted to a Gearman Job Server, then returns
     *         an empty array.
     */
    byte[] getHandle();

    /**
     * Retrieves a unique ID used to identify this job. The ID is created when
     * the Job is created, as opposed to the handle which is created by the
     * Job Server once the job has been submitted.
     *
     * @return A unique used to identify this job.
     */
    byte[] getID();

    /**
     * Retrieve the name of the {@link org.gearman.worker.GearmanFunction}
     * registered with this job.
     *
     * @return function name
     */
    String getFunctionName();

    /**
     * Determines if this job is a background job. A background job is a job
     * which does not send status updates back to the {@link GearmanClient}
     * that submitted the job. Unlike non-background jobs, executing
     * {@link Future#get()} will not block until job completion. Nor will a
     * background job return a {@link GearmanJobResult} when it has finished
     * execution. To determine the status of background job, you must execute
     * {@link GearmanClient#getJobStatus(org.gearman.client.GearmanJob) }.
     *
     * @return true if the job is a background job, else returns false.
     */
    boolean isBackgroundJob();

    /**
     * Retrieve the priority associated with this job.
     *
     * @return Job Priority
     */
    JobPriority getPriority();

    /**
     * Retrieve the data against which the job is executed. This data is
     * contained in {@link org.gearman.worker.GearmanFunction} that has been
     * registered with this job.
     *
     * @return The data against which the job is executed or if no data has
     *         been specified, returns an empty array.
     */
    byte[] getData();

    /**
     * Registers a particular {@link org.gearman.worker.GearmanFunction} with
     * this job. The GearmanFunction contains the definition for how the job
     * will be executed and the data against which the execution will occurr.
     *
     * @param function The function defining job execution and data.
     */
    void registerFunction(Callable<GearmanJobResult> function);

    /**
     * As a <tt>GearmanJob</tt> progress through its' lifecycle, it will
     * receives a series of events notifications from the Gearman Job Server.
     * For example when the job has been successfully submitted to the sever, it
     * will receive a JOB_CREATED notification, or when some intermediate data
     * has been sent back from the worker, than a WORK_DATA event is generated.
     *
     * <p>
     * This method lets user register listeners who will receive notifications
     * when a particular event has been received from the Gearman Job Server.
     * See {@link GearmanIOEventListener} for the list of events that can
     * listened for and handled by the listener.
     *
     * @param listener the listener to which event notifications will be sent.
     */
    void registerEventListener(GearmanIOEventListener listener);

    /**
     * removes a {@link GearmanIOEventListener} from recieving event
     * notifications.
     *
     * @param listener the listener to be removed.
     * @return true if the listener has been removed, else false.
     */
    boolean removeEventListener(GearmanIOEventListener listener);
}
