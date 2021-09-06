package io.runtime.mcumgr.transfer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.response.UploadResponse;

@SuppressWarnings("unused")
public abstract class Upload extends Transfer {

    private final UploadCallback mCallback;

    protected Upload(byte @NotNull [] data) {
        this(data, null);
    }

    protected Upload(byte @NotNull [] data, @Nullable UploadCallback callback) {
        super(data);
        mCallback = callback;
    }

    protected abstract UploadResponse write(byte @NotNull [] data, int offset) throws McuMgrException;

    @Override
    public McuMgrResponse send(int offset) throws McuMgrException {
        if (mData == null) {
            throw new NullPointerException("Upload data cannot be null!");
        }
        UploadResponse response = write(mData, offset);
        // Check for a McuManager error.
        if (response.rc != 0) {
            throw new McuMgrErrorException(McuMgrErrorCode.valueOf(response.rc));
        }

        mOffset = response.off;

        return response;
    }

    @Override
    public void reset() {
        mOffset = 0;
    }

    @Override
    public void onProgressChanged(int current, int total, long timestamp) {
        if (mCallback != null) {
            mCallback.onUploadProgressChanged(current, total, timestamp);
        }
    }

    @Override
    public void onFailed(@NotNull McuMgrException e) {
        if (mCallback != null) {
            mCallback.onUploadFailed(e);
        }
    }

    @Override
    public void onCompleted() {
        if (mCallback != null) {
            mCallback.onUploadCompleted();
        }
    }

    @Override
    public void onCanceled() {
        if (mCallback != null) {
            mCallback.onUploadCanceled();
        }
    }
}
