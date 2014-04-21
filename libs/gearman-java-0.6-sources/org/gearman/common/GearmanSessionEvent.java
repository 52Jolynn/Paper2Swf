/*
 * Copyright (C) 2009 by Eric Lambert <Eric.Lambert@sun.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman.common;

public class GearmanSessionEvent {

    final GearmanPacket packet;
    final GearmanJobServerSession session;

    public GearmanSessionEvent(GearmanPacket packet,
            GearmanJobServerSession session) {
        this.packet = packet;
        this.session = session;
    }

    public GearmanPacket getPacket() {
        return packet;
    }

    public GearmanJobServerSession getSession() {
        return session;
    }
}
