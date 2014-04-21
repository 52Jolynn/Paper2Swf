/*
 * Copyright (C) 2009 by Eric Herman <eric@freesa.org>
 * Use and distribution licensed under the
 * GNU Lesser General Public License (LGPL) version 2.1.
 * See the COPYING file in the parent directory for full text.
 */
package org.gearman.util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

public final class ByteUtils {

    public static final byte NULL = (byte) 0;
    public static final byte[] EMPTY = new byte[0];
    public static final String CHARSET_ASCII = "ASCII";
    public static final String CHARSET_UTF_8 = "UTF-8";

    private ByteUtils() {
        
    }

    public static byte[] toAsciiBytes(String string) {
        return toBytes(string, CHARSET_ASCII);
    }

    public static String fromAsciiBytes(byte[] bytes) {
        return fromBytes(bytes, CHARSET_ASCII);
    }

    public static byte[] toUTF8Bytes(String string) {
        return toBytes(string, CHARSET_UTF_8);
    }

    public static String fromUTF8Bytes(byte[] bytes) {
        return fromBytes(bytes, CHARSET_UTF_8);
    }

    public static byte[] toBytes(String string, String encoding) {
        try {
            return string.getBytes(encoding);
        } catch (UnsupportedEncodingException noAscii) {
            throw new IllegalArgumentException("Runtime does not support" +
                    " encoding " + encoding, noAscii);
        }
    }

    public static String fromBytes(byte[] bytes, String encoding) {
        if (bytes == null) {
            return null;
        }
        try {
            return new String(bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Runtime does not support" +
                    " encoding " + encoding, e);
        }
    }

    public static String toHex(final byte bytes[]) {
        if (bytes == null) {
            return null;
        }
        if (bytes.length == 0) {
            return "";
        }
        String hexFormat = "%0" + (bytes.length * 2) + "x";
        return String.format(hexFormat, new BigInteger(1, bytes));
    }

    public static byte[] fromHex(final String hex) {
        return new BigInteger(hex, 16).toByteArray();
    }

    public static byte[] toBigEndian(int i) {
        byte b0 = selectByte(3, i);
        byte b1 = selectByte(2, i);
        byte b2 = selectByte(1, i);
        byte b3 = selectByte(0, i);
        return new byte[]{b0, b1, b2, b3};
    }

    public static int fromBigEndian(byte[] b) {
        return toInt(3, b[0]) //
                + toInt(2, b[1]) //
                + toInt(1, b[2]) //
                + toInt(0, b[3]);
    }

    public static byte selectByte(int byteSignificance, int from) {
        return (byte) (from >> (8 * byteSignificance));
    }

    public static int toInt(int byteSignificance, byte b) {
        return ((b & 0xFF) << (8 * byteSignificance));
    }

    /**
     * null-safe byte[] copy
     */
    public static byte[] copy(byte[] orig) {
        if (orig == null) {
            return ByteUtils.EMPTY;
        }
        byte[] copy = new byte[orig.length];
        System.arraycopy(orig, 0, copy, 0, copy.length);
        return copy;
    }
}
