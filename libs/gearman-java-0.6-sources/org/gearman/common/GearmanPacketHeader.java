/*
 * Copyright (C) 2009 by Eric Herman <eric@freesa.org>
 * Use and distribution licensed under the
 * GNU Lesser General Public License (LGPL) version 2.1.
 * See the COPYING file in the parent directory for full text.
 */
package org.gearman.common;

import java.io.OutputStream;

import org.gearman.util.ByteArrayBuffer;
import org.gearman.util.ByteUtils;
import org.gearman.util.IOUtil;

public class GearmanPacketHeader {
    /*
     * HEADER
     *
     * 4 byte magic code - This is either "\0REQ" for requests or "\0RES"for
     * responses.
     *
     * 4 byte type - A big-endian (network-order) integer containing an
     * enumerated packet type. Possible values are:
     *
     * 4 byte size - A big-endian (network-order) integer containing the size of
     * the data being sent after the header.
     */

    public static final int HEADER_LENGTH = 12;
    private GearmanPacketMagic magic;
    private GearmanPacketType type;
    private int dataLength;

    public GearmanPacketHeader(GearmanPacketMagic magic,
            GearmanPacketType type, int dataLength) {
        this.magic = magic;
        this.type = type;
        this.dataLength = dataLength;
    }

    public GearmanPacketHeader(byte[] bytes) {
        ByteArrayBuffer baBuff = new ByteArrayBuffer(bytes);
        magic = GearmanPacketMagic.fromBytes(baBuff.subArray(0, 4));
        int typeInt = ByteUtils.fromBigEndian(baBuff.subArray(4, 8));
        type = GearmanPacketType.get(typeInt);
        dataLength = ByteUtils.fromBigEndian(baBuff.subArray(8, 12));
    }

    public GearmanPacketMagic getMagic() {
        return magic;
    }

    public void setMagic(GearmanPacketMagic magic) {
        this.magic = magic;
    }

    public GearmanPacketType getType() {
        return type;
    }

    public void setType(GearmanPacketType type) {
        this.type = type;
    }

    public int getDataLength() {
        return dataLength;
    }

    public void setDataLength(int dataLength) {
        this.dataLength = dataLength;
    }

    public void write(OutputStream os) {
        IOUtil.write(os, magic.toBytes());
        IOUtil.write(os, type.toBytes());
        IOUtil.write(os, getDataSizeBytes());
    }

    /*
     * 4 byte size - A big-endian (network-order) integer
     */
    private byte[] getDataSizeBytes() {
        return ByteUtils.toBigEndian(dataLength);
    }
}
