/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr;


import org.jetbrains.annotations.NotNull;

import io.runtime.mcumgr.util.ByteUtil;
import io.runtime.mcumgr.util.Endian;

/**
 * The Mcu Manager header is an 8-byte array which identifies the specific command and provides
 * fields for optional values such as flags and sequence numbers. This class is used to parse
 * and build headers.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class McuMgrHeader {
    public final static int HEADER_LENGTH = 8;

    private int mOp;
    private int mFlags;
    private int mLen;
    private int mGroupId;
    private int mSequenceNum;
    private int mCommandId;

    public McuMgrHeader(int op, int flags, int len, int groupId, int sequenceNum, int commandId) {
        mOp = op;
        mFlags = flags;
        mLen = len;
        mGroupId = groupId;
        mSequenceNum = sequenceNum;
        mCommandId = commandId;
    }

    public byte[] toBytes() {
        return build(mOp, mFlags, mLen, mGroupId, mSequenceNum, mCommandId);
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

    @Override
    public String toString() {
        return "Header (Op: " + mOp + ", Flags: " + mFlags + ", Len: " + mLen + ", Group: " + mGroupId + ", Seq: " + mSequenceNum + ", Command: " + mCommandId + ")";
    }

    public static McuMgrHeader fromBytes(byte[] header) {
        if (header == null || header.length != HEADER_LENGTH) {
            return null;
        }
        int op          = ByteUtil.unsignedByteArrayToInt(header, 0, 1, Endian.BIG);
        int flags       = ByteUtil.unsignedByteArrayToInt(header, 1, 1, Endian.BIG);
        int len         = ByteUtil.unsignedByteArrayToInt(header, 2, 2, Endian.BIG);
        int groupId     = ByteUtil.unsignedByteArrayToInt(header, 4, 2, Endian.BIG);
        int sequenceNum = ByteUtil.unsignedByteArrayToInt(header, 6, 1, Endian.BIG);
        int commandId   = ByteUtil.unsignedByteArrayToInt(header, 7, 1, Endian.BIG);
        return new McuMgrHeader(op, flags, len, groupId, sequenceNum, commandId);
    }

    /**
     * Builds a new manager header.
     *
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
    @NotNull
    public static byte[] build(int op, int flags, int len, int group, int sequence, int id) {
        return new byte[]{
                (byte) op,
                (byte) flags,
                (byte) (len >>> 8), (byte) len,
                (byte) (group >>> 8), (byte) group,
                (byte) sequence,
                (byte) id};
    }
}
