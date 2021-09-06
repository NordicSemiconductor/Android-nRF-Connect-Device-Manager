/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.image;

import org.jetbrains.annotations.NotNull;

import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.util.ByteUtil;
import io.runtime.mcumgr.util.Endian;

/**
 * Represents a firmware image version for devices using McuBoot or the legacy Apache Mynewt
 * bootloader.
 * <p>
 * For more info about McuBoot and image format see:
 * <a href="https://juullabs-oss.github.io/mcuboot/design.html">https://juullabs-oss.github.io/mcuboot/design.html</a>
 */
@SuppressWarnings("unused")
public class McuMgrImageVersion {

    private final byte mMajor;
    private final byte mMinor;
    private final short mRevision;
    private final int mBuildNum;

    private McuMgrImageVersion(byte major, byte minor, short revision, int buildNum) {
        mMajor = major;
        mMinor = minor;
        mRevision = revision;
        mBuildNum = buildNum;
    }

    @NotNull
    public static McuMgrImageVersion fromBytes(@NotNull byte[] b) throws McuMgrException {
        return fromBytes(b, 0);
    }

    @NotNull
    public static McuMgrImageVersion fromBytes(@NotNull byte[] b, int offset) throws McuMgrException {
        if (b.length - offset < getSize()) {
            throw new McuMgrException("The byte array is too short to be a McuMgrImageVersion");
        }

        byte major = b[offset++];
        byte minor = b[offset++];
        short revision = (short) ByteUtil.byteArrayToUnsignedInt(b, offset, Endian.LITTLE, 2);
        int buildNum = ByteUtil.byteArrayToUnsignedInt(b, offset + 2, Endian.LITTLE, 4);

        return new McuMgrImageVersion(major, minor, revision, buildNum);
    }

    public static int getSize() {
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
