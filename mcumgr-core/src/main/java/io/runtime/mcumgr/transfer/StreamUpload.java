package io.runtime.mcumgr.transfer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.response.UploadResponse;

@SuppressWarnings("unused")
public abstract class StreamUpload extends StreamTransfer {

    @NotNull
    private final InputStream mDataInput;

    private final UploadCallback mCallback;

    protected StreamUpload(@NotNull InputStream data, int totalBytes) {
        this(data, totalBytes, null);
    }

    protected StreamUpload(
            @NotNull InputStream data,
            int totalBytes,
            @Nullable UploadCallback callback
    ) {
        super(0, totalBytes);
        mDataInput = data;
        mCallback = callback;
    }

    protected abstract UploadResponse write(@NotNull InputStream data, int offset, int totalBytes) throws McuMgrException;

    @Override
    public McuMgrResponse send(int offset) throws McuMgrException {
        UploadResponse response = write(mDataInput, offset, mDataLength);
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
