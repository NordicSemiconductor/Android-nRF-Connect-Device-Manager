package io.runtime.mcumgr;

import java.nio.ByteBuffer;

import io.runtime.mcumgr.util.ByteUtil;


public class McuMgrHeader {

    public final static int NMGR_HEADER_LEN = 8;

    private int mOp;
    private int mFlags;
    private int mLen;
    private int mGroupId;
    private int mSequenceNum;
    private int mCommandId;

    public McuMgrHeader(int op, int flags, int len, int groupId, int sequenceNum, int commandId) {
        mOp = op;
        mFlags = flags;
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

    public static McuMgrHeader fromBytes(byte[] header) {
        if (header.length != NMGR_HEADER_LEN) {
            return null;
        }
        int op = ByteUtil.unsignedByteArrayToInt(header, 0, 1);
        int flags = ByteUtil.unsignedByteArrayToInt(header, 1, 1);
        int len = ByteUtil.unsignedByteArrayToInt(header, 2, 2);
        int groupId = ByteUtil.unsignedByteArrayToInt(header, 4, 2);
        int sequenceNum = ByteUtil.unsignedByteArrayToInt(header, 6, 1);
        int commandId = ByteUtil.unsignedByteArrayToInt(header, 7, 1);
        return new McuMgrHeader(op, flags, len, groupId, sequenceNum, commandId);
    }

    /**
     * Builds a new manager header.
     * @param op the operation for this packet: NMGR_WRITE, NMGR_WRITE_RSP, NMGR_READ, NMGR_READ_RSP
     * @param flags newt manager flags
     * @param len the length (this field is NOT required for all default newt manager commands)
     * @param group the newt manager command group. Some groups such as GROUP_IMAGE must also
     *              specify a subcommand ID.
     * @param sequence the newt manager sequence number
     * @param id the subcommand ID for certain groups
     * @return the built newt manager header
     */
    public static byte[] build(int op, int flags, int len, int group, int sequence, int id) {
        byte[] hdr = {(byte)op, (byte)flags, 0, 0, 0, 0, (byte)sequence, (byte)id};
        ByteBuffer.wrap(hdr).putShort(4, (short)group).array();
        ByteBuffer.wrap(hdr).putShort(2, (short)len).array();
        return hdr;
    }
}
