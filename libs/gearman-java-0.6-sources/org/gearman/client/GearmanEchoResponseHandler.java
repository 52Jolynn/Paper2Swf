/*
 * Copyright (C) 2009 by Eric Lambert <Eric.Lambert@sun.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman.client;

import org.gearman.common.GearmanException;
import org.gearman.common.GearmanPacket;
import org.gearman.common.GearmanPacketType;
import org.gearman.common.GearmanServerResponseHandler;

public class GearmanEchoResponseHandler implements GearmanServerResponseHandler {

    private static final String DESCRIPTION = "GearmanEcho";
    byte[] data = null;
    boolean done = false;

    public void handleEvent(GearmanPacket response) throws GearmanException {
        GearmanPacketType pt = response.getPacketType();
        if (!pt.equals(GearmanPacketType.ECHO_RES)) {
            throw new GearmanException("Dont know how to handle response of" +
                    " type " + pt);
        }
        data = response.getData();
        done = true;
    }

    public boolean isDone() {
        return done;
    }

    @Override
    public String toString() {
        return DESCRIPTION;
    }

    byte[] getResults() {
        byte[] ret = new byte[0];
        if (done) {
            ret = new byte[data.length];
            System.arraycopy(data, 0, ret, 0, ret.length);
        }
        return ret;
    }
}
