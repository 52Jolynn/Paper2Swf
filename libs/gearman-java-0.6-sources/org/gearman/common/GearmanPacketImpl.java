/*
 * Copyright (C) 2009 by Eric Herman <eric@freesa.org>
 * Use and distribution licensed under the
 * GNU Lesser General Public License (LGPL) version 2.1.
 * See the COPYING file in the parent directory for full text.
 *
 * Copyright (C) 2009 by Eric Lambert <Eric.Lambert@sun.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 * 
 */
package org.gearman.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.gearman.util.ByteUtils;
import org.gearman.util.IORuntimeException;
import org.gearman.util.IOUtil;

/**
 * Requests and responses sent between workers or clients and job servers are
 * contained in binary packets. The packet header begins with a magic code that
 * indicates whether a packet is a request or a response. The packet also has a
 * type, which the receiver uses to determine what additional data should be in
 * the packet, as well as how to interpret the data.
 */
public class GearmanPacketImpl implements GearmanPacket {


    private final GearmanPacketMagic magic;

    private final GearmanPacketType type;

    private final byte[] data;

    public GearmanPacketImpl(GearmanPacketMagic magic,
            GearmanPacketType type, byte[] data) {
        this.magic = magic;
        this.type = type;
        this.data = ByteUtils.copy(data);
    }

    public GearmanPacketImpl(InputStream in) {
        byte[] bytes = new byte[GearmanPacketHeader.HEADER_LENGTH];

        blockUntilReadFully(in, bytes);

        GearmanPacketHeader header = new GearmanPacketHeader(bytes);
        byte[] inputData = new byte[header.getDataLength()];
        if (inputData.length > 0) {
            IOUtil.readFully(in, inputData);
        }
        this.magic = header.getMagic();
        this.type = header.getType();
        this.data = inputData;
    }

    /**
     * @return a copy of the array
     */
    public byte[] getData() {
        return ByteUtils.copy(data);
    }

    /**
     * @return the length in bytes of the data
     */
    public int getDataSize() {
        return data.length;
    }

    public GearmanPacketType getPacketType() {
        return type;
    }

    public GearmanPacketMagic getMagic() {
        return magic;
    }

    public byte[] toBytes() {
        int totalSize = getDataSize() + Constants.GEARMAN_PACKET_HEADER_SIZE;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(totalSize);
        write(baos);
        IOUtil.flush(baos);
        return baos.toByteArray();
    }

    ArrayList<byte[]> getDataComponents(int tokens) {                           //NOPMD
        int curTokenStart, i, curToken;
        curTokenStart = i = curToken = 0;
        byte[] curTokenData = null;
        ArrayList<byte[]> al = new ArrayList<byte[]>(tokens);

        if (tokens == 0) {
            return al;
        }

        // process all but the last token (data segment)
        while (curToken < tokens - 1 && i < data.length) {
            if (data[i] == ByteUtils.NULL) {
                curTokenData = new byte[i - curTokenStart];                     //NOPMD
                System.arraycopy(data, curTokenStart, curTokenData, 0,
                        i - curTokenStart);
                al.add(curTokenData);
                curTokenStart = i + 1;
                curToken++;
            }
            i++;
        }

        // copy the last token
        curTokenData = new byte[data.length - i];
        System.arraycopy(data, curTokenStart, curTokenData, 0, data.length - i);
        al.add(curTokenData);
        return al;
    }


    /**
     * Writes the complete packet to the specified OutputStream.
     * 
     * @param os
     */
    public void write(OutputStream os) {
        /*
         * HEADER
         */
        new GearmanPacketHeader(magic, type, getDataSize()).write(os);

        /*
         * DATA
         * 
         * Arguments given in the data part are separated by a NULL byte, and
         * the last argument is determined by the size of data after the last
         * NULL byte separator. All job handle arguments must not be longer than
         * 64 bytes, including NULL terminator.
         */
        IOUtil.write(os, data);
    }

    public GearmanPacketType getType() {
        return type;
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer(magic + ":" + type + ":" +
                data.length);
        if (data.length > 0) {
            s.append(": [" + ByteUtils.toHex(data) + "]");
        }
        return s.toString();
    }

    public boolean requiresResponse() {
        if (magic.equals(GearmanPacketMagic.RES)) {
            return false;
        }
        if (type.equals(GearmanPacketType.PRE_SLEEP) ||
                type.equals(GearmanPacketType.GRAB_JOB) ||
                type.equals(GearmanPacketType.GRAB_JOB_UNIQ) ||
                type.equals(GearmanPacketType.GET_STATUS) ||
                type.equals(GearmanPacketType.ECHO_REQ) ||
                type.equals(GearmanPacketType.SUBMIT_JOB) ||
                type.equals(GearmanPacketType.SUBMIT_JOB_BG) ||
                type.equals(GearmanPacketType.SUBMIT_JOB_LOW) ||
                type.equals(GearmanPacketType.SUBMIT_JOB_LOW_BG) ||
                type.equals(GearmanPacketType.SUBMIT_JOB_HIGH) ||
                type.equals(GearmanPacketType.SUBMIT_JOB_HIGH_BG) ||
                type.equals(GearmanPacketType.OPTION_REQ)) {
            return true;
        }
        return false;
    }

    public byte[] getDataComponentValue(DataComponentName component) {
        HashMap<GearmanPacket.DataComponentName,byte []> valuesMap =
                new HashMap<GearmanPacket.DataComponentName,byte[]>();
        ArrayList<byte []> components = null;
        switch (type) {
            case ECHO_REQ:
            case ECHO_RES:
                components = getDataComponents(1);
                valuesMap.put(DataComponentName.DATA, components.get(0));
                break;
            case ERROR:
                components = getDataComponents(2);
                valuesMap.put(DataComponentName.ERROR_CODE, components.get(0));
                valuesMap.put(DataComponentName.ERROR_TEXT, components.get(1));
                break;
            case SUBMIT_JOB:
            case SUBMIT_JOB_BG:
            case SUBMIT_JOB_LOW:
            case SUBMIT_JOB_LOW_BG:
            case SUBMIT_JOB_HIGH:
            case SUBMIT_JOB_HIGH_BG:
                components = getDataComponents(3);
                valuesMap.put(DataComponentName.FUNCTION_NAME, components.get(0));
                valuesMap.put(DataComponentName.UNIQUE_ID, components.get(1));
                valuesMap.put(DataComponentName.DATA, components.get(2));
                break;
            case SUBMIT_JOB_SCHED:
                components = getDataComponents(8);
                valuesMap.put(DataComponentName.FUNCTION_NAME, components.get(0));
                valuesMap.put(DataComponentName.UNIQUE_ID, components.get(1));
                valuesMap.put(DataComponentName.MINUTE, components.get(2));
                valuesMap.put(DataComponentName.HOUR, components.get(3));
                valuesMap.put(DataComponentName.DAY_OF_MONTH, components.get(4));
                valuesMap.put(DataComponentName.MONTH, components.get(5));
                valuesMap.put(DataComponentName.DAY_OF_WEEK, components.get(6));
                valuesMap.put(DataComponentName.DATA, components.get(7));
                break;
            case SUBMIT_JOB_EPOCH:
                components = getDataComponents(4);
                valuesMap.put(DataComponentName.FUNCTION_NAME, components.get(0));
                valuesMap.put(DataComponentName.UNIQUE_ID, components.get(1));
                valuesMap.put(DataComponentName.EPOCH, components.get(2));
                valuesMap.put(DataComponentName.DATA, components.get(3));
                break;
            case GET_STATUS:
            case JOB_CREATED:
            case WORK_FAIL:
                components = getDataComponents(1);
                valuesMap.put(DataComponentName.JOB_HANDLE, components.get(0));
                break;
            case OPTION_REQ:
            case OPTION_RES:
                components = getDataComponents(1);
                valuesMap.put(DataComponentName.OPTION, components.get(0));
                break;
            case CAN_DO:
            case CANT_DO:
                components = getDataComponents(1);
                valuesMap.put(DataComponentName.FUNCTION_NAME, components.get(0));
                break;
            case CAN_DO_TIMEOUT:
                components = getDataComponents(2);
                valuesMap.put(DataComponentName.FUNCTION_NAME, components.get(0));
                valuesMap.put(DataComponentName.TIME_OUT, components.get(1));
                break;
            case WORK_DATA:
            case WORK_WARNING:
            case WORK_COMPLETE:
            case WORK_EXCEPTION:
                components = getDataComponents(2);
                valuesMap.put(DataComponentName.JOB_HANDLE, components.get(0));
                valuesMap.put(DataComponentName.DATA, components.get(1));
                break;
            case WORK_STATUS:
                components = getDataComponents(3);
                valuesMap.put(DataComponentName.JOB_HANDLE, components.get(0));
                valuesMap.put(DataComponentName.NUMERATOR, components.get(1));
                valuesMap.put(DataComponentName.DENOMINATOR, components.get(2));
                break;
            case STATUS_RES:
                components = getDataComponents(5);
                valuesMap.put(DataComponentName.JOB_HANDLE, components.get(0));
                valuesMap.put(DataComponentName.KNOWN_STATUS, components.get(1));
                valuesMap.put(DataComponentName.RUNNING_STATUS, components.get(2));
                valuesMap.put(DataComponentName.NUMERATOR, components.get(3));
                valuesMap.put(DataComponentName.DENOMINATOR, components.get(4));
                break;
            case SET_CLIENT_ID:
                components = getDataComponents(1);
                valuesMap.put(DataComponentName.CLIENT_ID, components.get(0));
                break;
            case JOB_ASSIGN:
                components = getDataComponents(3);
                valuesMap.put(DataComponentName.JOB_HANDLE, components.get(0));
                valuesMap.put(DataComponentName.FUNCTION_NAME, components.get(1));
                valuesMap.put(DataComponentName.DATA, components.get(2));
                break;
             case JOB_ASSIGN_UNIQ:
                components = getDataComponents(4);
                valuesMap.put(DataComponentName.JOB_HANDLE, components.get(0));
                valuesMap.put(DataComponentName.FUNCTION_NAME, components.get(1));
                valuesMap.put(DataComponentName.UNIQUE_ID, components.get(2));
                valuesMap.put(DataComponentName.DATA, components.get(3));
                break;
            case RESET_ABILITIES:
            case PRE_SLEEP:
            case GRAB_JOB:
            case GRAB_JOB_UNIQ:
            case ALL_YOURS:
            case NOOP:
                break;
            default:
                throw new IllegalArgumentException("Unknown packet type " +
                        type);
        }
        byte [] rt = valuesMap.containsKey(component) ? valuesMap.get(component) :
            new byte[0];
        return rt;
    }

    public static byte [] generatePacketData(byte[]... data) {
        int len = 0;
        int curPos = 0;
        for (byte [] curArray : data) {
            len += curArray.length + 1;
        }
        len--;
        byte [] rdata = new byte[len];
        for (byte [] curArray : data) {
            System.arraycopy(curArray, 0, rdata, curPos, curArray.length);
            curPos += curArray.length;
            if (curPos < rdata.length) {
                rdata[curPos++] = ByteUtils.NULL;
            }
        }
        return rdata;
    }

    private void blockUntilReadFully(InputStream in, byte[] bytes) {
        try {
            while (in.available() < GearmanPacketHeader.HEADER_LENGTH) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        IOUtil.readFully(in, bytes);
    }
}
