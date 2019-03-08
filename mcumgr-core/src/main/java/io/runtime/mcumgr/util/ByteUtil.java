/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.util;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"unused", "WeakerAccess"})
public class ByteUtil {

    public static final int BYTE_WIDTH  = 1;
    public static final int SHORT_WIDTH = 2;
    public static final int INT_WIDTH   = 4;
    public static final int LONG_WIDTH  = 8;

    //******************************************************************
    // Int
    //******************************************************************

    /**
     * Converts a unsigned byte to an int.
     *
     * @param b the byte.
     * @return The unsigned int.
     */
    public static int byteToUnsignedInt(byte b) {
        return b & 0xff;
    }

    /**
     * Converts a byte array to an unsigned integer.
     * <p>
     * Parses the first four bytes of data (big endian) as an unsigned int.
     *
     * @param data the byte array.
     * @return The unsigned integer.
     */
    public static int byteArrayToUnsignedInt(@NotNull byte[] data) {
        return byteArrayToUnsignedInt(data, 0, Endian.BIG, INT_WIDTH);
    }

    /**
     * Converts a byte array to an unsigned integer.
     * <p>
     * Parses the first four bytes of data (big endian) from the offset as an unsigned integer.
     *
     * @param data the byte array.
     * @param offset the offset to start parsing the int.
     * @return The unsigned integer.
     */
    public static int byteArrayToUnsignedInt(@NotNull byte[] data, int offset) {
        return byteArrayToUnsignedInt(data, offset, Endian.BIG, INT_WIDTH);
    }


    /**
     * Converts a byte array to an unsigned integer.
     * <p>
     * Parses the next four bytes of data (big endian) from the offset as an unsigned integer.
     *
     * @param data the byte array
     * @param offset the offset to start parsing the int
     * @param endian the endianness of the data. ({@link Endian#LITTLE} or {@link Endian#BIG}).
     *               If null, {@link Endian#BIG} is assumed
     * @return The unsigned integer.
     */
    public static int byteArrayToUnsignedInt(@NotNull byte[] data,
                                             int offset,
                                             @Nullable Endian endian) {
        return byteArrayToUnsignedInt(data, offset, endian, INT_WIDTH);
    }

    /**
     * Converts a byte array to an unsigned integer.
     *
     * @param data   the byte array.
     * @param offset the offset to start parsing the int.
     * @param endian the endianness of the data. ({@link Endian#LITTLE} or {@link Endian#BIG}).
     *               If null, {@link Endian#BIG} is assumed
     * @param length the number of bytes to convert to an int. This number must be between 0 and 4.
     * @return The unsigned integer.
     */
    public static int byteArrayToUnsignedInt(@NotNull byte[] data,
                                             int offset,
                                             @Nullable Endian endian,
                                             int length) {
        if (length < 0 || length > 4) {
            throw new IllegalArgumentException("Length must be between 0 and 4 inclusive (length=" +
                    length + ").");
        }
        int result = 0;
        for (int i = 0; i < length; i++) {
            int unsignedByte = byteToUnsignedInt(data[i + offset]);
            if (endian == null || endian == Endian.BIG) {
                result += unsignedByte << ((length - 1 - i) * 8); // big endian
            } else {
                result += unsignedByte << (i * 8); // little endian
            }
        }
        return result;
    }

    //******************************************************************
    // Long
    //******************************************************************

    /**
     * Converts a byte to an unsigned long.
     *
     * @param b the byte.
     * @return The unsigned long.
     */
    public static long byteToUnsignedLong(byte b) {
        return b & 0xff;
    }

    /**
     * Converts a byte array to an unsigned long.
     * <p>
     * Parses the first four bytes of data (big endian) as an unsigned long.
     *
     * @param data the byte array.
     * @return The unsigned long.
     */
    public static long byteArrayToUnsignedLong(@NotNull byte[] data) {
        return byteArrayToUnsignedLong(data, 0, Endian.BIG, LONG_WIDTH);
    }

    /**
     * Converts a byte array to an unsigned long.
     * <p>
     * Parses the first four bytes of data (big endian) from the offset as an unsigned long.
     *
     * @param data the byte array.
     * @param offset the offset to start parsing the long.
     * @return The unsigned long.
     */
    public static long byteArrayToUnsignedLong(@NotNull byte[] data, int offset) {
        return byteArrayToUnsignedLong(data, offset, Endian.BIG, LONG_WIDTH);
    }


    /**
     * Converts a byte array to an unsigned long.
     * <p>
     * Parses the next four bytes of data (big endian) from the offset as an unsigned long.
     *
     * @param data the byte array
     * @param offset the offset to start parsing the long
     * @param endian the endianness of the data. ({@link Endian#LITTLE} or {@link Endian#BIG}).
     *               If null, {@link Endian#BIG} is assumed
     * @return The unsigned long.
     */
    public static long byteArrayToUnsignedLong(@NotNull byte[] data,
                                             int offset,
                                             @Nullable Endian endian) {
        return byteArrayToUnsignedLong(data, offset, endian, LONG_WIDTH);
    }

    /**
     * Converts a byte array to an unsigned long.
     *
     * @param data   the byte array.
     * @param offset the offset to start parsing the long.
     * @param endian the endianness of the data. ({@link Endian#LITTLE} or {@link Endian#BIG}).
     *               If null, {@link Endian#BIG} is assumed
     * @param length the number of bytes to convert to a long. This number must be between 0 and 4.
     * @return The unsigned long.
     */
    public static long byteArrayToUnsignedLong(@NotNull byte[] data,
                                             int offset,
                                             @Nullable Endian endian,
                                             int length) {
        if (length < 0 || length > 4) {
            throw new IllegalArgumentException("Length must be between 0 and 4 inclusive (length=" +
                    length + ").");
        }
        long result = 0;
        for (int i = 0; i < length; i++) {
            int unsignedByte = byteToUnsignedInt(data[i + offset]);
            if (endian == null || endian == Endian.BIG) {
                result += unsignedByte << ((length - 1 - i) * 8); // big endian
            } else {
                result += unsignedByte << (i * 8); // little endian
            }
        }
        return result;
    }


    //******************************************************************
    // String
    //******************************************************************

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
