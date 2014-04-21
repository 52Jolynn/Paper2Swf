/*
 * Copyright (C) 2009 by Eric Lambert <Eric.Lambert@sun.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman.common;

public interface GearmanServerResponseHandler {

    public void handleEvent(GearmanPacket event) throws GearmanException;

    public boolean isDone();
}
