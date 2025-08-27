/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr;


import org.jetbrains.annotations.NotNull;

import no.nordicsemi.android.mcumgr.util.ByteUtil;
import no.nordicsemi.android.mcumgr.util.Endian;

/**
 * The Mcu Manager header is an 8-byte array which identifies the specific command and provides
 * fields for optional values such as flags and sequence numbers. This class is used to parse
 * and build headers.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class McuMgrHeader {

    public final static int HEADER_LENGTH = 8;

    private int mVersion;
    private int mOp;
    private int mFlags;
    private int mLen;
    private int mGroupId;
    private int mSequenceNum;
    private int mCommandId;

    public McuMgrHeader(int version, int op, int flags, int len, int groupId, int sequenceNum, int commandId) {
        mVersion = version;
        mOp = op;
        mFlags = flags;
        mLen = len;
        mGroupId = groupId;
        mSequenceNum = sequenceNum;
        mCommandId = commandId;
    }

    public byte @NotNull [] toBytes() {
        return build(mVersion, mOp, mFlags, mLen, mGroupId, mSequenceNum, mCommandId);
    }

    public int getVersion() {
        return mVersion;
    }

    public void setVersion(int version) {
        this.mVersion = version;
    }

    public int getOp() {
        return mOp;
    }

    public void setOp(int op) {
        this.mOp = op;
    }

    public int getFlags() {
        return mFlags;
    }

    public void setFlags(int flags) {
        this.mFlags = flags;
    }

    public int getLen() {
        return mLen;
    }

    public void setLen(int len) {
        this.mLen = len;
    }

    public int getGroupId() {
        return mGroupId;
    }

    public void setGroupId(int groupId) {
        this.mGroupId = groupId;
    }

    public int getSequenceNum() {
        return mSequenceNum;
    }

    public void setSequenceNum(int sequenceNum) {
        this.mSequenceNum = sequenceNum;
    }

    public int getCommandId() {
        return mCommandId;
    }

    public void setCommandId(int commandId) {
        this.mCommandId = commandId;
    }

    @NotNull
    @Override
    public String toString() {
        return "Header (Version: " + mVersion + ", Op: " + mOp + ", Flags: " + mFlags + ", Len: " + mLen + ", Group: " +
                mGroupId + ", Seq: " + mSequenceNum + ", Command: " + mCommandId + ")";
    }

    /**
     * Parse the mcumgr header from a byte array.
     * This function will parse the first 8 bytes from the array, discounting any additional bytes.
     *
     * @param header the byte array to parse the header from.
     * @return The parsed mcumgr header.
     * @throws IllegalArgumentException when the byte array length is less than 8 bytes
     */
    @NotNull
    public static McuMgrHeader fromBytes(byte @NotNull [] header) {
        if (header.length < HEADER_LENGTH) {
            throw new IllegalArgumentException("Failed to parse mcumgr header from bytes; too short - length=" + header.length);
        }
        int meta        = ByteUtil.byteArrayToUnsignedInt(header, 0, Endian.BIG, 1);
        int version     = (meta >> 3) & 0b11;
        int op          = meta & 0b111;
        int flags       = ByteUtil.byteArrayToUnsignedInt(header, 1, Endian.BIG, 1);
        int len         = ByteUtil.byteArrayToUnsignedInt(header, 2, Endian.BIG, 2);
        int groupId     = ByteUtil.byteArrayToUnsignedInt(header, 4, Endian.BIG, 2);
        int sequenceNum = ByteUtil.byteArrayToUnsignedInt(header, 6, Endian.BIG, 1);
        int commandId   = ByteUtil.byteArrayToUnsignedInt(header, 7, Endian.BIG, 1);
        return new McuMgrHeader(version, op, flags, len, groupId, sequenceNum, commandId);
    }

    /**
     * Builds a new manager header.
     *
     * @param version  this indicates the version of the protocol being used, this should be set to
     *                 0b01 to use the newer SMP transport where error codes are more detailed and
     *                 returned in the map, otherwise left as 0b00 to use the legacy SMP protocol.
     *                 Versions 0b10 and 0b11 are reserved for future use and should not be used.
     * @param op       the operation for this packet: ({@link McuManager#OP_READ OP_READ},
     *                 {@link McuManager#OP_READ_RSP OP_READ_RSP}, {@link McuManager#OP_WRITE OP_WRITE},
     *                 {@link McuManager#OP_WRITE_RSP OP_WRITE_RSP}).
     * @param flags    newt manager flags.
     * @param len      the length (this field is NOT required for all default newt manager commands).
     * @param group    the newt manager command group. Some groups such as GROUP_IMAGE must also
     *                 specify a sub-command ID.
     * @param sequence the newt manager sequence number.
     * @param id       the sub-command ID for certain groups.
     * @return The built newt manager header.
     */
    public static byte @NotNull [] build(int version, int op, int flags, int len, int group, int sequence, int id) {
        return new byte[]{
                (byte) ((op & 0b111) | ((version & 0b11) << 3)),
                (byte) flags,
                (byte) (len >>> 8), (byte) len,
                (byte) (group >>> 8), (byte) group,
                (byte) sequence,
                (byte) id};
    }
}
