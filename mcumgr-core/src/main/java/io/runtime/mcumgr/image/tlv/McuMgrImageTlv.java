/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.image.tlv;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.image.McuMgrImageHeader;

/**
 * Represents a type-length-value table for firmware images using McuBoot or the legacy Apache
 * Mynewt bootloader.
 * <p>
 * For more info about McuBoot and image format see:
 * <a href="https://juullabs-oss.github.io/mcuboot/design.html">https://juullabs-oss.github.io/mcuboot/design.html</a>
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class McuMgrImageTlv {

    // See link below for more info on the image TLV types
    // https://github.com/mcu-tools/mcuboot/blob/9331c924ba69a32e142d1bf724443d99405c3323/boot/bootutil/include/bootutil/image.h#L95

    /** Hash of the public key. Used for legacy versions of mcuboot. */
    public final static int IMG_TLV_SHA256_V1 = 0x01;
    /** SHA256 of image hdr and body */
    public final static int IMG_TLV_SHA256 = 0x10;
    /** SHA384 of image hdr and body */
    public final static int IMG_TLV_SHA384 = 0x11;
    /** SHA512 of image hdr and body */
    public final static int IMG_TLV_SHA512 = 0x12;
    /** RSA2048 of hash output */
    public final static int IMG_TLV_RSA2048_PSS = 0x20;
    /** ECDSA of hash output */
    public final static int IMG_TLV_ECDSA224 = 0x21;
    /** ECDSA of hash output */
    public final static int IMG_TLV_ECDSA256 = 0x22;
    /** RSA3072 of hash output */
    public final static int IMG_TLV_RSA3072_PSS = 0x23;
    /** ED25519 of hash output */
    public final static int IMG_TLV_ED25519 = 0x24;
    /** Key encrypted with RSA-OAEP-2048 */
    public final static int IMG_TLV_ENC_RSA2048 = 0x30;
    /** Key encrypted with AES-KW-128 */
    public final static int IMG_TLV_ENC_KW128 = 0x31;
    /** Key encrypted with ECIES P256 */
    public final static int IMG_TLV_ENC_EC256 = 0x32;
    /** Image depends on other image */
    public final static int IMG_TLV_DEPENDENCY = 0x40;

    /** Magic number for the unprotected TLV */
    public final static int IMG_TLV_INFO_MAGIC = 0x6907;
    /** Magic number for the protected TLV */
    public final static int IMG_TLV_PROTECTED_INFO_MAGIC = 0x6908;

    @Nullable
    private final McuMgrImageTlvInfo mTlvInfo;
    @NotNull
    private final List<McuMgrImageTlvTrailerEntry> mTrailerEntries;
    private final boolean mIsLegacy;
    private final int mSize;

    private McuMgrImageTlv(@NotNull McuMgrImageTlvInfo tlvInfo,
                           @NotNull ArrayList<McuMgrImageTlvTrailerEntry> entries) {
        mIsLegacy = false;
        mTlvInfo = tlvInfo;
        mTrailerEntries = entries;
        mSize = tlvInfo.getTotal();
    }

    /*
     * Legacy Constructor
     */
    private McuMgrImageTlv(@NotNull ArrayList<McuMgrImageTlvTrailerEntry> entries, int size) {
        mIsLegacy = true;
        mTlvInfo = null;
        mTrailerEntries = entries;
        mSize = size;
    }

    @Nullable
    public McuMgrImageTlvInfo getTlvInfo() {
        return mTlvInfo;
    }

    @NotNull
    public List<McuMgrImageTlvTrailerEntry> getTrailerEntries() {
        return mTrailerEntries;
    }

    public boolean isLegacy() {
        return mIsLegacy;
    }

    public int getSize() {
        return mSize;
    }

    public boolean isProtected() {
        if (mIsLegacy || mTlvInfo == null) {
            return false;
        }
        return mTlvInfo.isProtected();
    }

    public byte @Nullable [] getHash() {
        for (McuMgrImageTlvTrailerEntry entry : getTrailerEntries()) {
            if (entry.type == IMG_TLV_SHA512)
                return entry.value;
            if (entry.type == IMG_TLV_SHA384)
                return entry.value;
            if (mIsLegacy && entry.type == IMG_TLV_SHA256_V1 ||
               !mIsLegacy && entry.type == IMG_TLV_SHA256) {
                return entry.value;
            }
        }
        return null;
    }

    public static McuMgrImageTlv fromBytes(byte[] data, int offset, boolean isLegacy)
            throws McuMgrException {

        McuMgrImageTlvInfo tlvInfo = null;
        ArrayList<McuMgrImageTlvTrailerEntry> entries = new ArrayList<>();

        int end;

        // If image is legacy, skip the tlv info
        if (isLegacy) {
            end = data.length;
        } else {
            tlvInfo = McuMgrImageTlvInfo.fromBytes(data, offset);
            offset += McuMgrImageTlvInfo.getSize();
            end = offset + tlvInfo.getTotal();
        }

        // Parse each trailer entry
        while (offset + McuMgrImageTlvTrailerEntry.getMinSize() < end) {
            McuMgrImageTlvTrailerEntry tlvEntry = McuMgrImageTlvTrailerEntry.fromBytes(data, offset);
            entries.add(tlvEntry);
            offset += tlvEntry.getEntryLength();
        }

        if (isLegacy) {
            return new McuMgrImageTlv(entries, offset - end);
        } else {
            return new McuMgrImageTlv(tlvInfo, entries);
        }
    }

    @Deprecated
    public static McuMgrImageTlv fromBytes(byte[] data, McuMgrImageHeader header)
            throws McuMgrException {
        int offset = header.getHdrSize() + header.getImgSize();
        return fromBytes(data, offset, header.isLegacy());
    }
}
