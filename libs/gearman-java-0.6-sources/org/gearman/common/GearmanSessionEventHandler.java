/*
 * Copyright (C) 2009 by Eric Lambert <Eric.Lambert@sun.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman.common;

public interface GearmanSessionEventHandler {

    public void handleSessionEvent(GearmanSessionEvent event)
            throws IllegalArgumentException, IllegalStateException;
}
