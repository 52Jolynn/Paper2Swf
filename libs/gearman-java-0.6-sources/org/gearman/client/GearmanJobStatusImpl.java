/*
 * Copyright (C) 2009 by Eric Lambert <Eric.Lambert@sun.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman.client;

import org.gearman.common.Constants;
import org.gearman.common.GearmanException;
import org.gearman.common.GearmanPacket;
import org.gearman.common.GearmanPacketType;
import org.gearman.common.GearmanServerResponseHandler;
import org.gearman.util.ByteUtils;
import org.slf4j.LoggerFactory;

public class GearmanJobStatusImpl implements GearmanServerResponseHandler,
        GearmanJobStatus {

    private static final String DESCRIPTION = "GearmanJobStatus";
    private boolean isRunning = false;
    private boolean isKnown = false;
    private long denominator = 0;
    private long numerator = 0;
    private boolean statusRequestCompleted = false;
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(
            Constants.GEARMAN_SESSION_LOGGER_NAME);

    GearmanJobStatusImpl() {
        super();
    }

    public long getDenominator() {
        return denominator;
    }

    public long getNumerator() {
        return numerator;
    }

    public boolean isKnown() {
        return isKnown;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void handleEvent(GearmanPacket repsonse) throws GearmanException {
        if (!repsonse.getPacketType().equals(GearmanPacketType.STATUS_RES)) {
            throw new GearmanException("Dont know how to handle response of " +
                    "type " + repsonse);
        }

        isKnown = (repsonse.getDataComponentValue(
                GearmanPacket.DataComponentName.KNOWN_STATUS))[0] == '0' ?
                    false : true;
        isRunning = (repsonse.getDataComponentValue(
                GearmanPacket.DataComponentName.RUNNING_STATUS))[0] == '0' ?
                    false : true;
        //The legal values of Numerator and Denominator are a bit unclear, it is
        //possible for a job to send back non-numerical values, for now we
        //swallow this since the JobResult and JobStatus classes expect these
        //values to be longs
        try {
            numerator = Long.parseLong(ByteUtils.fromUTF8Bytes(
                repsonse.getDataComponentValue(
                GearmanPacket.DataComponentName.NUMERATOR)));
        } catch (NumberFormatException nfe) {
            LOG.warn("numerator for response " + this + " has non-numeric value ");
        }
        try {
            denominator = Long.parseLong(ByteUtils.fromUTF8Bytes(
                repsonse.getDataComponentValue(
                GearmanPacket.DataComponentName.DENOMINATOR)));
        } catch (NumberFormatException nfe) {
            LOG.warn("denominator for response " + this + " has non-numeric value ");
        }
        statusRequestCompleted = true;
    }

    public boolean isDone() {
        return statusRequestCompleted;
    }

    @Override
    public String toString() {
        return DESCRIPTION;
    }
}
