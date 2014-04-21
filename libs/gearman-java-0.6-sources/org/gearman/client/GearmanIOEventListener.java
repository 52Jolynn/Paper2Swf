/*
 * Copyright (C) 2009 by Eric Lambert <Eric.Lambert@sun.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman.client;

import org.gearman.common.GearmanPacket;

/**
 * A <tt>GearmanIOEventListener</tt> receives notifications as events are
 * sent from the Gearman Job Server to a particular {@link GearmanJob}. When the
 * job receives an event, it passes itself to all registered listeners and
 * executes the appropriate method,thereby giving the listener a chance to
 * handle the event.
 *
 */
public interface GearmanIOEventListener {

    /**
     * The method to be called when an event has occurred.
     *
     * @param event The fired event.
     */
    public void handleGearmanIOEvent(GearmanPacket event) throws
            IllegalArgumentException;
}
