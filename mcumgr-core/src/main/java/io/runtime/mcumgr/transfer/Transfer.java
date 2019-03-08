package io.runtime.mcumgr.transfer;

import org.jetbrains.annotations.Nullable;

import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.McuMgrResponse;

public abstract class Transfer implements TransferCallback {

    @Nullable
    byte[] mData;
    int mOffset;

    Transfer(@Nullable byte[] data, int offset) {
        mData = data;
        mOffset = offset;
    }

    public abstract void reset();

    public abstract McuMgrResponse send(int offset) throws McuMgrException;

    public McuMgrResponse sendNext() throws McuMgrException {
        return send(mOffset);
    }

    @Nullable
    public byte[] getData() {
        return mData;
    }

    public void setData(@Nullable byte[] data) {
        mData = data;
    }

    public int getOffset() {
        return mOffset;
    }

    public void setOffset(int offset) {
        mOffset = offset;
    }

    public boolean isFinished() {
        return mData != null && mOffset == mData.length;
    }
}
