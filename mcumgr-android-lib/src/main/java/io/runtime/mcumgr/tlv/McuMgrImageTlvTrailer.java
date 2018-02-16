/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.tlv;

import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.util.ByteUtil;

public class McuMgrImageTlvTrailer {
    public static final int IMAGE_TLV_SHA256 = 0x10;
    public static final int IMAGE_HASH_LEN = 32;

    private byte mType;
    private byte mPad;
    private short mLen;

    private McuMgrImageTlvTrailer() {

    }

    public static int sizeof() {
        return 1 + 1 + 2;
    }

    public static McuMgrImageTlvTrailer fromBytes(byte[] b) throws McuMgrException {
        return fromBytes(b, 0);
    }

    public static McuMgrImageTlvTrailer fromBytes(byte[] b, int offset) throws McuMgrException {
        if (b.length - offset < sizeof()) {
            throw new McuMgrException("The byte array is too short to be a McuMgrImageTlvTrailer");
        }

        McuMgrImageTlvTrailer trailer = new McuMgrImageTlvTrailer();
        trailer.mType = b[offset++];
        offset++; // pad
        trailer.mLen = (short) ByteUtil.unsignedByteArrayToInt(b, offset, 2);

        return trailer;
    }

    public byte getType() {
        return mType;
    }

    public byte getPad() {
        return mPad;
    }

    public short getLen() {
        return mLen;
    }
}
