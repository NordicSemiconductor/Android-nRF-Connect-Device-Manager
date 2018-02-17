package io.runtime.mcumgr.img;

import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.img.tlv.McuMgrImageTlv;

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
