/*
 * Copyright (C) 2009 by Eric Herman <eric@freesa.org>
 * Use and distribution licensed under the
 * GNU Lesser General Public License (LGPL) version 2.1.
 * See the COPYING file in the parent directory for full text.
 */
package org.gearman.common;

import org.gearman.util.ByteUtils;

/**
 * Enum for the different magic codes that can appear at the beginning of a
 * packet header.
 */
public enum GearmanPacketMagic {

    REQ("REQ"), RES("RES");

    public static class BadMagicException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private BadMagicException(String hex) {
            super(hex);
        }
    }
    private byte[] name;

    private GearmanPacketMagic(String kind) {
        this.name = new byte[1 + kind.length()];
        this.name[0] = 0;
        byte[] bytes = ByteUtils.toUTF8Bytes(kind);
        System.arraycopy(bytes, 0, this.name, 1, kind.length());
    }

    /**
     * @return the PacketMagic bytes
     */
    public byte[] toBytes() {
        byte [] retBytes = new byte[name.length];
        System.arraycopy(name, 0, retBytes, 0, name.length);
        return retBytes;
    }

    /**
     * "\0REQ" == [ 00 52 45 51 ] == 5391697
     *
     * "\0RES" == [ 00 52 45 53 ] == 5391699
     *
     * @param bytes
     *          array of bytes containing magic code for a packet
     * @throws BadMagicException if byte array is not 4 bytes long
     */
    public static GearmanPacketMagic fromBytes(byte[] bytes) {
        if (bytes != null && bytes.length == 4) {
            int magic = ByteUtils.fromBigEndian(bytes);
            if (magic == ByteUtils.fromBigEndian(REQ.toBytes())) {
                return REQ;
            }
            if (magic == ByteUtils.fromBigEndian(RES.toBytes())) {
                return RES;
            }
        }
        throw new BadMagicException(ByteUtils.toHex(bytes));
    }
}
