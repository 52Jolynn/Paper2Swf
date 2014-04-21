/*
 * Copyright (C) 2009 by Eric Herman <eric@freesa.org>
 * Copyright (C) 2009 by Robert Stewart
 * Use and distribution licensed under the
 * GNU Lesser General Public License (LGPL) version 2.1.
 * See the COPYING file in the parent directory for full text.
 */
package org.gearman.common;

import org.gearman.util.ByteUtils;

public enum GearmanPacketType {

    NULL(0), //
    CAN_DO(1), //
    CANT_DO(2), //
    RESET_ABILITIES(3), //
    PRE_SLEEP(4), //
    unused_5(5), //
    NOOP(6), //
    SUBMIT_JOB(7), //
    JOB_CREATED(8), //
    GRAB_JOB(9), //
    NO_JOB(10), //
    JOB_ASSIGN(11), //
    WORK_STATUS(12), //
    WORK_COMPLETE(13), //
    WORK_FAIL(14), //
    GET_STATUS(15), //
    ECHO_REQ(16), //
    ECHO_RES(17), //
    SUBMIT_JOB_BG(18), //
    ERROR(19), //
    STATUS_RES(20), //
    SUBMIT_JOB_HIGH(21), //
    SET_CLIENT_ID(22), //
    CAN_DO_TIMEOUT(23), //
    ALL_YOURS(24), //
    WORK_EXCEPTION(25), //
    OPTION_REQ(26), //
    OPTION_RES(27), //
    WORK_DATA(28), //
    WORK_WARNING(29), //
    GRAB_JOB_UNIQ(30), //
    JOB_ASSIGN_UNIQ(31), //
    SUBMIT_JOB_HIGH_BG(32), //
    SUBMIT_JOB_LOW(33), //
    SUBMIT_JOB_LOW_BG(34), //
    SUBMIT_JOB_SCHED(35), //
    SUBMIT_JOB_EPOCH(36);

    /*
     * A big-endian (network-job) integer containing an enumerated packet type.
     */
    private final byte[] type;
    private final int code;

    private GearmanPacketType(int i) {
        type = ByteUtils.toBigEndian(i);
        code = i;
    }

    public byte[] toBytes() {
        byte [] retBytes = new byte[type.length];
        System.arraycopy(type, 0, retBytes, 0, type.length);
        return retBytes;
    }

    public static boolean isJobSubmission(GearmanPacketType pt) {
        if (pt.equals(SUBMIT_JOB) || pt.equals(SUBMIT_JOB_BG) ||
            pt.equals(SUBMIT_JOB_HIGH) || pt.equals(SUBMIT_JOB_HIGH_BG) ||
            pt.equals(SUBMIT_JOB_LOW) || pt.equals(SUBMIT_JOB_LOW_BG)) {
            return true;
        }
        return false;
    }

    /**
     * Returns the PacketType for the specified <code>ordinal</code>.
     *
     * @param ordinal
     *            a PacketType ordinal
     * @return the PacketType for the specified <code>ordinal</code>
     * @throws IllegalArgumentException
     *             if an invalid ordinal is provided
     */
    public static GearmanPacketType get(int ordinal) {
        for (GearmanPacketType type : GearmanPacketType.values()) {
            /* we could use <code>type.ordinal()</code> here, too. */
            if (type.code == ordinal) {
                return type;
            }
        }
        throw new IllegalArgumentException(Integer.toString(ordinal));
    }
}
