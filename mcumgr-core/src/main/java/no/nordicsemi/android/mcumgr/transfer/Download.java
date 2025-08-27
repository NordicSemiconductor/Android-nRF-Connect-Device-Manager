package no.nordicsemi.android.mcumgr.transfer;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nordicsemi.android.mcumgr.McuMgrErrorCode;
import no.nordicsemi.android.mcumgr.exception.McuMgrErrorException;
import no.nordicsemi.android.mcumgr.exception.McuMgrException;
import no.nordicsemi.android.mcumgr.response.DownloadResponse;
import no.nordicsemi.android.mcumgr.response.McuMgrResponse;

@SuppressWarnings("unused")
public abstract class Download extends Transfer {
    private final static Logger LOG = LoggerFactory.getLogger(Download.class);

    @Nullable
    private final DownloadCallback mCallback;

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
            mDataLength = response.len;
        }
        if (mData == null) {
            throw new McuMgrException("Download buffer is null, packet with offset 0 was never received.");
        }

        // Validate response body
        if (response.data == null || response.data.length == 0) {
            throw new McuMgrException("Download response data is empty.");
        }
        final int length = Math.min(response.data.length, mData.length - response.off);
        if (length <= 0) {
            throw new McuMgrException("Download offset too big: " + response.off + " (file length: " + mData.length + ", received: " + response.data.length + ")");
        }
        if (length != response.data.length) {
            LOG.warn("Received more data than expected. Expected: {}, received: {}", length, response.data.length);
        }

        // Copy received mData to the buffer.
        System.arraycopy(response.data, 0, mData, response.off, length);
        mOffset = response.off + response.data.length;

        return response;
    }

    @Override
    public void reset() {
        mOffset = 0;
        mData = null;
        mDataLength = -1;
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
