/*
 * Copyright (C) 2013 by Eric Lambert <eric.d.lambert@gmail.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman.worker;

import java.util.concurrent.Callable;
import org.gearman.client.GearmanIOEventListener;
import org.gearman.client.GearmanJobResult;
import org.gearman.common.GearmanPacket;

/**
 *
 * A Gearman Function represents the work to be accomplished by a
 * {@link org.gearman.client.GearmanJob}. It defines how the job is executed
 * and the data, if any, against which that execution will occurr.
 */
public interface GearmanFunction extends Callable<GearmanJobResult> {

    /**
     * Retrieves the name of the funcion to be executed by the job. This is
     * used by a {@link GearmanWorker} to determine if the function has been
     * regisitered with the worker and therefor can be executed by the Worker.
     *
     * @return the name of the function.
     */
    String getName();

    /**
     * Some functions require a dataset upon which to operate. This method is
     * used to load that data so that it is available when the function is
     * called.
     *
     * @param data The data used by the function at execution time.
     */
    void setData(Object data);

    /**
     * Set the handle of the job for which this function is executing.
     *
     * @param handle the job handle.
     * @throws IllegalArgumentException if the handle is null or empty.
     */
    void setJobHandle(byte [] handle) throws IllegalArgumentException;

    /**
     * Retrieves the handle for the job that is executing this function.
     *
     * @return The jobhandle or an empty array if the handle is not set.
     */
    byte [] getJobHandle();

    /**
     * As a function executes, it can generate a series of I/O Events -- in the
     * form of a {@link GearmanPacket} -- that can be handled by a series of
     * listeners. This method registers an instance of a listener with this
     * function. When the function calls the
     * {@link #fireEvent(org.gearman.common.GearmanPacket) } all registered
     * listeners should be given a chance to handle the event.
     *
     * An exampe of this would be a function that generates intermediate data.
     * As the data is generated, the function can call this method to allow
     * any listeners to act upon that data.
     *
     * @param listener
     * @throws IllegalArgumentException if the listener is null or deemed to be
     * invalid by the function.
     */
    void registerEventListener(GearmanIOEventListener listener)
            throws IllegalArgumentException;

    /**
     * Allows all {@link GearmanIOEventListener} registered with this function
     * to handle the specified event.
     *
     * @param event The event to be handled by the listeners.
     */
    void fireEvent(GearmanPacket event);

	/**
	 * Set the Unique ID given to this job by the client
	 */
	void setUniqueId(byte[] uuid);

	/**
	 * Returns the Unique ID given to this job by the client
	 */
	byte[] getUniqueId();

}
