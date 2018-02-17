/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.img.tlv;

import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.util.ByteUtil;

public class McuMgrImageTlvInfo {
    private short mMagic;
    private short mTotal;

    private McuMgrImageTlvInfo() {

    }

    public static McuMgrImageTlvInfo fromBytes(byte[] b) throws McuMgrException {
        return fromBytes(b, 0);
    }

    public static McuMgrImageTlvInfo fromBytes(byte[] b, int offset) throws McuMgrException {
        if (b.length - offset < getSize())
            throw new McuMgrException("The byte array is too short to be a McuMgrImageTlvInfo: " +
                    "length= " + b.length + ", offset= " + offset + ", min size= " + getSize());

        McuMgrImageTlvInfo info = new McuMgrImageTlvInfo();
        info.mMagic = (short) ByteUtil.unsignedByteArrayToInt(b, offset, 2);
        info.mTotal = (short) ByteUtil.unsignedByteArrayToInt(b, offset + 2, 2);

        if (info.mMagic != McuMgrImageTlv.IMG_TLV_INFO_MAGIC) {
            throw new McuMgrException("Wrong magic number, magic= " + info.mMagic + " instead of " +
                    McuMgrImageTlv.IMG_TLV_INFO_MAGIC);
        }
        return info;
    }

    public static int getSize() {
        return 4;
    }

    public short getMagic() {
        return mMagic;
    }

    public short getTotal() {
        return mTotal;
    }
}
