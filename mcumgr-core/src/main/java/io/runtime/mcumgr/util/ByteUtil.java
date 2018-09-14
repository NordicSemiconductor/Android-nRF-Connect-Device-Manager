/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.util;


@SuppressWarnings({"unused", "WeakerAccess"})
public class ByteUtil {

    /**
     * Converts a unsigned byte to an int.
     *
     * @param b the unsigned byte.
     * @return The unsigned int.
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
     * @param data   the unsigned byte array.
     * @param offset the offset to start parsing the int.
     * @param length the length of the int to parse in bytes.
     * @param endian the endianness to use while parsing the array ({@link Endian#LITTLE} or
     *               {@link Endian#BIG}).
     * @return The int.
     */
    public static int unsignedByteArrayToInt(byte[] data, int offset, int length, Endian endian) {
        int result = 0;
        for (int i = 0; i < length; i++) {
            int unsignedByte = unsignedByteToInt(data[i + offset]);
            if (endian == Endian.BIG) {
                result += unsignedByte << ((length - 1 - i) * 8); // big endian
            } else {
                result += unsignedByte << (i * 8); // little endian
            }
        }
        return result;
    }

    public static String byteArrayToHex(byte[] a) {
        return byteArrayToHex(a, 0, a.length, "%02x ");
    }

    public static String byteArrayToHex(byte[] a, String format) {
        return byteArrayToHex(a, 0, a.length, format);
    }

    public static String byteArrayToHex(byte[] a, int offset, int length, String format) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (int i = offset; i < length; i++) {
            sb.append(String.format(format, a[i]));
        }
        return sb.toString();
    }
}
