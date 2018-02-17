/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.img;

import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.util.ByteUtil;

public class McuMgrImageVersion {
    private byte mMajor;
    private byte mMinor;
    private short mRevision;
    private int mBuildNum;

    private McuMgrImageVersion() {

    }

    public static McuMgrImageVersion fromBytes(byte[] b) throws McuMgrException {
        return fromBytes(b, 0);
    }

    public static McuMgrImageVersion fromBytes(byte[] b, int offset) throws McuMgrException {
        if (b.length - offset < sizeof()) {
            throw new McuMgrException("The byte array is too short to be a McuMgrImageVersion");
        }

        McuMgrImageVersion version = new McuMgrImageVersion();
        version.mMajor = b[offset++];
        version.mMinor = b[offset++];
        version.mRevision = (short) ByteUtil.unsignedByteArrayToInt(b, offset, 2);
        version.mBuildNum = ByteUtil.unsignedByteArrayToInt(b, offset + 2, 4);

        return version;
    }

    public static int sizeof() {
        return 1 + 1 + 2 + 4;
    }

    public byte getMajor() {
        return mMajor;
    }

    public byte getMinor() {
        return mMinor;
    }

    public short getRevision() {
        return mRevision;
    }

    public int getBuildNum() {
        return mBuildNum;
    }
}
