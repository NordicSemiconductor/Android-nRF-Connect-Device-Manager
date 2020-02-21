/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.image;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @NotNull
    private final McuMgrImageHeader mHeader;
    @Nullable
    private final McuMgrImageTlv mProtectedTlv;
    @NotNull
    private final McuMgrImageTlv mTlv;
    @NotNull
    private final byte[] mHash;
    @NotNull
    private final byte[] mData;

    public McuMgrImage(@NotNull McuMgrImageHeader header,
                       @Nullable McuMgrImageTlv protectedTlv,
                       @NotNull McuMgrImageTlv tlv,
                       @NotNull byte[] hash,
                       @NotNull byte[] data) {
        mHeader = header;
        mProtectedTlv = protectedTlv;
        mTlv = tlv;
        mHash = hash;
        mData = data;
    }

    @Deprecated
    public McuMgrImage(@NotNull byte[] data) throws McuMgrException {
        McuMgrImage image = fromBytes(data);
        mHeader = image.mHeader;
        mProtectedTlv = image.mProtectedTlv;
        mTlv = image.mTlv;
        mHash = image.mHash;
        mData = image.mData;
    }

    @NotNull
    public byte[] getData() {
        return mData;
    }

    @NotNull
    public McuMgrImageHeader getHeader() {
        return mHeader;
    }

    @Nullable
    public McuMgrImageTlv getProtectedTlv() {
        return mProtectedTlv;
    }

    @NotNull
    public McuMgrImageTlv getTlv() {
        return mTlv;
    }

    @NotNull
    public byte[] getHash() {
        return mHash;
    }

    @NotNull
    public static byte[] getHash(@NotNull byte[] data) throws McuMgrException {
        return fromBytes(data).getHash();
    }

    @NotNull
    public static McuMgrImage fromBytes(@NotNull byte[] data) throws McuMgrException {
        McuMgrImageHeader header = McuMgrImageHeader.fromBytes(data);
        int tlvOffset = header.getHdrSize() + header.getImgSize();
        McuMgrImageTlv tlv = McuMgrImageTlv.fromBytes(data, tlvOffset, header.isLegacy());
        McuMgrImageTlv protectedTlv = null;
        byte[] hash;
        if (tlv.isProtected()) {
            // If the first TLV is protected, we need to parse the next, unprotected TLV
            protectedTlv = tlv;
            tlv = McuMgrImageTlv.fromBytes(data, tlvOffset + protectedTlv.getSize(), header.isLegacy());
        }
        hash = tlv.getHash();
        if (hash == null) {
            throw new McuMgrException("Image TLV trailer does not contain an image hash!");
        }

        return new McuMgrImage(header, protectedTlv, tlv, hash, data);
    }
}
