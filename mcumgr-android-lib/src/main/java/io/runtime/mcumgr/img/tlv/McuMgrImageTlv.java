package io.runtime.mcumgr.img.tlv;

import java.util.ArrayList;
import java.util.List;

import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.img.McuMgrImageHeader;

public class McuMgrImageTlv {

    public final static int IMG_TLV_SHA256 = 0x10;
    public final static int IMG_TLV_SHA256_V1 = 0x01;
    public final static int IMG_TLV_INFO_MAGIC = 0x6907;

    private byte[] mData;
    private McuMgrImageHeader mHeader;

    private McuMgrImageTlvInfo mTlvInfo;
    private List<McuMgrImageTlvTrailerEntry> mTrailerEntries;

    private McuMgrImageTlv(byte[] data, McuMgrImageHeader header) {
        mData = data;
        mHeader = header;
        mTrailerEntries = new ArrayList<>();
    }

    public byte[] getData() {
        return mData;
    }

    public McuMgrImageHeader getHeader() {
        return mHeader;
    }

    public McuMgrImageTlvInfo getTlvInfo() {
        return mTlvInfo;
    }

    public List<McuMgrImageTlvTrailerEntry> getTrailerEntries() {
        return mTrailerEntries;
    }

    public byte[] getHash() {
        for (McuMgrImageTlvTrailerEntry entry : getTrailerEntries()) {
            if (mHeader.isLegacy() && entry.type == IMG_TLV_SHA256_V1 ||
                    !mHeader.isLegacy() && entry.type == IMG_TLV_SHA256) {
                return entry.value;
            }
        }
        return null;
    }

    public static McuMgrImageTlv fromBytes(byte[] data, McuMgrImageHeader header)
            throws McuMgrException {
        // Init Tlv
        McuMgrImageTlv tlv = new McuMgrImageTlv(data, header);

        int offset = header.getHdrSize() + header.getImgSize();
        int end = tlv.mData.length;

        // If image is legacy, skip the tlv info
        if (!header.isLegacy()) {
            tlv.mTlvInfo = McuMgrImageTlvInfo.fromBytes(tlv.mData, offset);
            offset += McuMgrImageTlvInfo.getSize();
        }

        // Parse each trailer item
        while (offset + McuMgrImageTlvTrailerEntry.getMinSize() < end) {
            McuMgrImageTlvTrailerEntry tlvEntry = McuMgrImageTlvTrailerEntry.fromBytes(data, offset);
            tlv.mTrailerEntries.add(tlvEntry);
            offset += tlvEntry.getEntryLength();
        }
        return tlv;
    }
}
