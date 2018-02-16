/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.util;

public class ByteUtil {

    /**
     * Converts a unsigned byte to an int.
     *
     * @param b the unsigned byte
     * @return the unsigned int
     */
    public static int unsignedByteToInt(byte b) {
        return b & 0xff;
    }

    /**
     * Converts a unsigned byte array to an unsigned integer.
     * <p>
     * Used to retrieve a number from a byte array of variable length. This method will not work
     * when length provided is greater than 4.
     *
     * @param data   the unsigned byte array
     * @param offset the offset to start parsing the int
     * @param length the length of the int to parse in bytes
     * @return the int
     */
    public static int unsignedByteArrayToInt(byte[] data, int offset, int length) {
        int result = 0;
        for (int i = 0; i < length; i++) {
            result += unsignedByteToInt(data[i + offset]) << (i * 8);
        }
        return result;
    }

    public static String byteArrayToHex(byte[] a) {
        return byteArrayToHex(a, "%02x ");
    }

    public static String byteArrayToHex(byte[] a, String format) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a)
            sb.append(String.format(format, b));
        return sb.toString();
    }

}
