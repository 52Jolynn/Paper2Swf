/*
 * Copyright (C) 2009 by Eric Lambert <Eric.Lambert@sun.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman.client;

/**
 * A <tt>GearmanJobStatus</tt> represents the state of a particular
 * {@link GearmanJob}, as known by Gearman Job Server that it was submitted to,
 * at a specific point in time.
 *
 */
public interface GearmanJobStatus {

    /**
     * Retrieves the denominator value of the {@link GearmanJob} at the time
     * the <tt>GearmanJobStatus</tt> was generated.
     *
     * @return denominator value.
     */
    long getDenominator();

    /**
     * Retrieves the numerator value of the {@link GearmanJob} at the time
     * the <tt>GearmanJobStatus</tt> was generated.
     *
     * @return numerator value.
     */
    long getNumerator();

    /**
     * Determine if the job is currently known by the Gearman Job Server to
     * which the {@link GearmanJob} was submitted. A job is known to the
     * Gearman Job Server if it has been submitted to the server and has not
     * yet completed.
     *
     * @return true if the job is known, else returns false.
     */
    boolean isKnown();

    /**
     * Determine if the job is currently running on a worker.
     *
     * @return true if the job is running, else returns false.
     */
    boolean isRunning();
}
