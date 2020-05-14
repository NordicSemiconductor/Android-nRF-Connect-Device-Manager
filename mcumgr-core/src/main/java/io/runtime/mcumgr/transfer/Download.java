package io.runtime.mcumgr.transfer;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.DownloadResponse;
import io.runtime.mcumgr.response.McuMgrResponse;

@SuppressWarnings("unused")
public abstract class Download extends Transfer {

    @Nullable
    private DownloadCallback mCallback;

    protected Download() {
        this(null);
    }

    protected Download(@Nullable DownloadCallback callback) {
        mCallback = callback;
    }

    /**
     * Sends read request from given offset.
     *
     * @param offset the offset.
     * @return received response.
     * @throws McuMgrException a reason of a failure.
     */
    protected abstract DownloadResponse read(int offset) throws McuMgrException;

    @Override
    public McuMgrResponse send(int offset) throws McuMgrException {
        DownloadResponse response = read(offset);
        // Check for a McuManager error.
        if (response.rc != 0) {
            throw new McuMgrErrorException(McuMgrErrorCode.valueOf(response.rc));
        }

        // The first packet contains the file length.
        if (response.off == 0) {
            mData = new byte[response.len];
        }

        // Validate response body
        if (response.data == null) {
            throw new McuMgrException("Download response data is null.");
        }
        if (mData == null) {
            throw new McuMgrException("Download data is null.");
        }

        // Copy received mData to the buffer.
        System.arraycopy(response.data, 0, mData, response.off, response.data.length);
        mOffset = response.off + response.data.length;

        return response;
    }

    @Override
    public void reset() {
        mOffset = 0;
        mData = null;
    }

    @Override
    public void onProgressChanged(int current, int total, long timestamp) {
        if (mCallback != null) {
            mCallback.onDownloadProgressChanged(current, total, timestamp);
        }
    }

    @Override
    public void onFailed(@NotNull McuMgrException e) {
        if (mCallback != null) {
            mCallback.onDownloadFailed(e);
        }
    }

    @Override
    public void onCompleted() {
        if (mCallback != null) {
            if (mData == null) {
                throw new NullPointerException("Transfer data cannot be null.");
            }
            mCallback.onDownloadCompleted(mData);
        }
    }

    @Override
    public void onCanceled() {
        if (mCallback != null) {
            mCallback.onDownloadCanceled();
        }
    }
}
