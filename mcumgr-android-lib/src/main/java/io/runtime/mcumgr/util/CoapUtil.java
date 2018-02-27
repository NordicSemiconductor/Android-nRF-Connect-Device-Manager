/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.util;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class CoapUtil {

    private final static String TAG = "CoapUtil";

    private final static byte PAYLOAD_MARKER = (byte) 0xff;

    /**
     * Gets the code class of a given CoAP code.
     *
     * @param code the code.
     * @return the value represented by the three most significant bits of the code.
     */
    public static int getCodeClass(final int code) {
        return (code & 0b11100000) >> 5;
    }

    /**
     * Gets the code detail of a given CoAP code.
     *
     * @param code the code.
     * @return the value represented by the five least significant bits of the code.
     */
    public static int getCodeDetail(final int code) {
        return code & 0b00011111;
    }

    /**
     * Get payload from a CoAP packet
     * @param data the CoAP packet data
     * @return the payload
     * @throws IOException Error parsing CoAP packet
     */
    public static byte[] getPayload(byte[] data) throws IOException {
        byte nextByte;
        int offset = 1 + getExtendedLengthLength(data) + 1 + getTokenLength(data);
        ByteArrayInputStream reader = new ByteArrayInputStream(data, offset, data.length - offset);

        while (reader.available() > 0) {
            try {
                nextByte = (byte) reader.read();
                if (nextByte != PAYLOAD_MARKER) {
                    // the first 4 bits of the byte represent the option delta
                    int optionDeltaNibble = (0xF0 & nextByte) >> 4;
                    determineValueFromNibble(optionDeltaNibble, reader);

                    // the second 4 bits represent the option length
                    int optionLengthNibble = 0x0F & nextByte;
                    int optionLength = determineValueFromNibble(optionLengthNibble, reader);

                    reader.read(new byte[optionLength]);
                } else {
                    byte[] payload = new byte[reader.available()];
                    reader.read(payload);
                    reader.close();
                    return payload;
                }
            } finally {
                reader.close();
            }
        }
        throw new IOException("Error parsing CoAP packet");
    }

    /**
     * Get the code from a CoAP packet
     * @param data the CoAP packet data
     * @return the code as an integer of format ((class * 100) + detail)
     */
    public static int getCode(byte[] data) {
        int rawCode = ByteUtil.unsignedByteToInt(data[1 + getExtendedLengthLength(data)]);
        int codeClass = getCodeClass(rawCode);
        int codeDetail = getCodeDetail(rawCode);
        return (codeClass * 100) + codeDetail;
    }

    private static int determineValueFromNibble(final int delta, ByteArrayInputStream reader) {
        if (delta <= 12) {
            return delta;
        } else if (delta == 13) {
            byte[] b = new byte[8];
            reader.read(b, 0, b.length);
            return ByteUtil.unsignedByteArrayToInt(b, 0, b.length, Endian.BIG) + 13;
        } else if (delta == 14) {
            byte[] b = new byte[16];
            reader.read(b, 0, b.length);
            return ByteUtil.unsignedByteArrayToInt(b, 0, b.length, Endian.BIG) + 269;
        }
        return delta;
    }

    public static byte[] getTokenFromData(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        int tokenOffset = 2;
        byte firstByte = data[0];
        int tokenLength = (firstByte & 0x0f);
        tokenOffset += getExtendedLengthLength(data);

        // Check if data is too small for token
        if (data.length < tokenOffset + tokenLength) {
            Log.e(TAG, "Data is too short to fit token - Data Length = " + data.length +
                    ", Min Length = " + (tokenOffset + tokenLength));
            return null;
        }
        byte[] token = new byte[tokenLength];
        for (int i = 0; i < token.length; i++) {
            token[i] = data[tokenOffset + i];
        }
        return token;
    }

    public static int getOptionsAndPayloadLength(byte[] data) {
        if (data == null || data.length == 0) {
            return 0;
        }
        int length = ByteUtil.unsignedByteToInt(data[0]) >>> 4;
        int extendedLengthLength = getExtendedLengthLength(data);
        int extendedLength = ByteUtil.unsignedByteArrayToInt(data, 1, extendedLengthLength, Endian.BIG);
        if (length == 13) {
            extendedLength += 13;
        } else if (length == 14) {
            extendedLength += 269;
        } else if (length == 15) {
            extendedLength += 65805;
        } else {
            extendedLength = length;
        }
        return extendedLength;
    }

    public static int getTokenLength(byte[] data) {
        if (data == null || data.length == 0) {
            return 0;
        }
        return data[0] & 0x0f;
    }

    public static int getPacketLength(byte[] data) {
        if (data == null || data.length == 0) {
            return 0;
        }
        return 1 + getExtendedLengthLength(data) + 1 + getTokenLength(data) + getOptionsAndPayloadLength(data);
    }

    public static int getExtendedLengthLength(byte[] data) {
        if (data == null || data.length == 0) {
            return 0;
        }
        byte firstByte = data[0];
        int length = (Byte.valueOf(firstByte).intValue() & 0x0000_00ff) >>> 4;
        if (length == 13) {
            return 1;
        } else if (length == 14) {
            return 2;
        } else if (length == 15) {
            return 4;
        }
        return 0;
    }

}
