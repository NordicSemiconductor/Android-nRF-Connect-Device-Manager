/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.image;

import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.image.tlv.McuMgrImageTlv;

/**
 * Represents a firmware image for devices using McuBoot or the legacy Apache Mynewt bootloader.
 * On initialization, the image data will be validated. A firmware image of this format contains an
 * image header and type-length-value trailer for image meta-data.
 * <p>
 * For more info about McuBoot and image format see:
 * <a href="https://runtimeco.github.io/mcuboot/design.html">https://runtimeco.github.io/mcuboot/design.html</a>
 */
public class McuMgrImage {

    public final static int IMG_HASH_LEN = 32;

    private McuMgrImageHeader mHeader;
    private McuMgrImageTlv mTlv;
    private byte[] mData;
    private byte[] mHash;

    public McuMgrImage(byte[] data) throws McuMgrException {
        mData = data;
        mHeader = McuMgrImageHeader.fromBytes(mData);
        mTlv = McuMgrImageTlv.fromBytes(mData, mHeader);
        mHash = mTlv.getHash();
        if (mHash == null) {
            throw new McuMgrException("Image TLV trailer does not contain an image hash!");
        }
    }

    public McuMgrImageHeader getHeader() {
        return mHeader;
    }

    public McuMgrImageTlv getTlv() {
        return mTlv;
    }

    public byte[] getHash() {
        return mHash;
    }

    public static byte[] getHash(byte[] data) throws McuMgrException {
        return new McuMgrImage(data).getHash();
    }
}
