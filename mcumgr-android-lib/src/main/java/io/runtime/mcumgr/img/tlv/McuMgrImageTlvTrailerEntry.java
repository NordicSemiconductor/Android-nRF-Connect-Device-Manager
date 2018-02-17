/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.img.tlv;

import java.util.Arrays;

import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.util.ByteUtil;

public class McuMgrImageTlvTrailerEntry {

    public final byte type;
    public final short length;
    public final byte[] value;

    private McuMgrImageTlvTrailerEntry(byte type, short length, byte[] value) {
        this.type = type;
        this.length = length;
        this.value = value;
    }

    public int getEntryLength() {
        return getMinSize() + length;
    }

    public static McuMgrImageTlvTrailerEntry fromBytes(byte[] b, int offset) throws McuMgrException {
        if (offset + getMinSize() > b.length) {
            throw new McuMgrException("The byte array is too short to be a McuMgrImageTlvTrailerEntry");
        }
        // Get type
        byte t = b[offset++];
        offset++; // Account for byte padding
        // Get length
        short l = (short) ByteUtil.unsignedByteArrayToInt(b, offset, 2);
        offset += 2; // Move past length
        // Get value
        byte[] v = Arrays.copyOfRange(b, offset, offset + l);
        return new McuMgrImageTlvTrailerEntry(t, l, v);
    }


    public static int getMinSize() {
        return 4;
    }
}
