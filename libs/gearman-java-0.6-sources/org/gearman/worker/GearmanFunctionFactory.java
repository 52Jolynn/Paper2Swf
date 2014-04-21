/*
 * Copyright (C) 2009 by Eric Herman <eric@freesa.org>
 * Copyright (C) 2009 by Robert Stewart <robert@wombatnation.com>
 * Copyright (C) 2009 by Eric Lambert <Eric.Lambert@sun.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman.worker;


/**
 * Factory for producing {@link GearmanFunction} objects. A factory can be
 * registered with a {@link GearmanWorker} to allow control over the JobFunction
 * instance that the Worker will call to perform {@link 
 * org.gearman.client.GearmanJob}s.
 */
public interface GearmanFunctionFactory {

    /**
     * Returns the name of the function for which this factory creates
     * {@link GearmanFunction} objects.
     *
     * @return name of the function for which this factory creates
     *         GearmanFunction objects
     */
    String getFunctionName();

    /**
     * Factory method for generating a {@link GearmanFunction} object.
     * Returns a GearmanFunction object that a Worker will call with a Job.
     *
     * @return GearmanFunction instance
     */
    GearmanFunction getFunction();
}
