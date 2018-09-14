/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.image;


import org.jetbrains.annotations.NotNull;

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
@SuppressWarnings({"WeakerAccess", "unused"})
public class McuMgrImage {
    public final static int IMG_HASH_LEN = 32;

    private final McuMgrImageHeader mHeader;
    private final McuMgrImageTlv mTlv;
    private final byte[] mData;
    private final byte[] mHash;

    public McuMgrImage(@NotNull byte[] data) throws McuMgrException {
        mData = data;
        mHeader = McuMgrImageHeader.fromBytes(data);
        mTlv = McuMgrImageTlv.fromBytes(data, mHeader);
        mHash = mTlv.getHash();
        if (mHash == null) {
            throw new McuMgrException("Image TLV trailer does not contain an image hash!");
        }
    }

    public byte[] getData() {
        return mData;
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

    public static byte[] getHash(@NotNull byte[] data) throws McuMgrException {
        return new McuMgrImage(data).getHash();
    }
}
