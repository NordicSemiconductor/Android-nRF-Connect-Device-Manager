/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.image;

import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.util.ByteUtil;
import io.runtime.mcumgr.util.Endian;

/**
 * Represents a firmware image header for devices using McuBoot or the legacy Apache Mynewt
 * bootloader.
 * <p>
 * For more info about McuBoot and image format see:
 * <a href="https://runtimeco.github.io/mcuboot/design.html">https://runtimeco.github.io/mcuboot/design.html</a>
 */
public class McuMgrImageHeader {
    private static final String TAG = McuMgrImageHeader.class.getSimpleName();

    public static int IMG_HEADER_MAGIC      = 0x96f3b83d;
    public static int IMG_HEADER_MAGIC_V1   = 0x96f3b83c;

    private static final int HEADER_LENGTH = 4 + 4 + 2 + 2 + 4 + 4 + 4;
    private int mMagic;
    private int mLoadAddr;
    private short mHdrSize;
    private short __mPad1;
    private int mImgSize;
    private int mFlags;
    private McuMgrImageVersion mVersion;
    private int __mPad2;

    private McuMgrImageHeader() {}

    public static McuMgrImageHeader fromBytes(byte[] b) throws McuMgrException {
        return fromBytes(b, 0);
    }

    public static McuMgrImageHeader fromBytes(byte[] b, int offset) throws McuMgrException {
        if (b.length - offset < getSize()) {
            throw new McuMgrException("The byte array is too short to be a McuMgrImageHeader");
        }

        McuMgrImageHeader header = new McuMgrImageHeader();
        header.mMagic = ByteUtil.unsignedByteArrayToInt(b, offset, 4, Endian.LITTLE);

        if (header.mMagic != IMG_HEADER_MAGIC && header.mMagic != IMG_HEADER_MAGIC_V1) {
            throw new McuMgrException("Wrong magic number: header=" + header.mMagic + ", magic=" +
                    IMG_HEADER_MAGIC + " or " + IMG_HEADER_MAGIC_V1);
        }

        header.mLoadAddr = ByteUtil.unsignedByteArrayToInt(b, 4 + offset, 4, Endian.LITTLE);
        header.mHdrSize = (short) ByteUtil.unsignedByteArrayToInt(b, 8 + offset, 2, Endian.LITTLE);
        header.mImgSize = ByteUtil.unsignedByteArrayToInt(b, 12 + offset, 4, Endian.LITTLE);
        header.mFlags = ByteUtil.unsignedByteArrayToInt(b, 16 + offset, 4, Endian.LITTLE);
        header.mVersion = McuMgrImageVersion.fromBytes(b, 20 + offset);

        return header;
    }

    public static int getSize() {
        return McuMgrImageVersion.getSize() + HEADER_LENGTH;
    }

    public int getMagic() {
        return mMagic;
    }

    public int getLoadAddr() {
        return mLoadAddr;
    }

    public short getHdrSize() {
        return mHdrSize;
    }

    public int getImgSize() {
        return mImgSize;
    }

    public int getFlags() {
        return mFlags;
    }

    public McuMgrImageVersion getVersion() {
        return mVersion;
    }

    public boolean isLegacy() {
        return mMagic == IMG_HEADER_MAGIC_V1;
    }
}
