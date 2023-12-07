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
 * <a href="https://juullabs-oss.github.io/mcuboot/design.html">https://juullabs-oss.github.io/mcuboot/design.html</a>
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class McuMgrImage implements ImageWithHash {
    public final static int IMG_HASH_LEN = 32;

    @NotNull
    private final McuMgrImageHeader mHeader;
    @Nullable
    private final McuMgrImageTlv mProtectedTlv;
    @NotNull
    private final McuMgrImageTlv mTlv;

    private final byte @NotNull [] mHash;
    private final byte @NotNull [] mData;

    public McuMgrImage(@NotNull McuMgrImageHeader header,
                       @Nullable McuMgrImageTlv protectedTlv,
                       @NotNull McuMgrImageTlv tlv,
                       byte @NotNull [] hash,
                       byte @NotNull [] data) {
        mHeader = header;
        mProtectedTlv = protectedTlv;
        mTlv = tlv;
        mHash = hash;
        mData = data;
    }

    @Deprecated
    public McuMgrImage(byte @NotNull [] data) throws McuMgrException {
        McuMgrImage image = fromBytes(data);
        mHeader = image.mHeader;
        mProtectedTlv = image.mProtectedTlv;
        mTlv = image.mTlv;
        mHash = image.mHash;
        mData = image.mData;
    }

    @Override
    public byte @NotNull [] getData() {
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

    @Override
    public byte @NotNull [] getHash() {
        return mHash;
    }

    @Override
    public boolean needsConfirmation() {
        // Actually, not all images require confirmation, but all require a reset.
        // If an  image is sent to a MCUboot in "DirectXIP without revert" mode if doesn't get
        // confirmed, but the Reset command is sent after uploading the image.
        return true;
    }

    public static byte @NotNull [] getHash(byte @NotNull [] data) throws McuMgrException {
        return fromBytes(data).getHash();
    }

    @NotNull
    public static McuMgrImage fromBytes(byte @NotNull [] data) throws McuMgrException {
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
            throw new McuMgrException("Image TLV trailer does not contain an image hash");
        }

        return new McuMgrImage(header, protectedTlv, tlv, hash, data);
    }
}
