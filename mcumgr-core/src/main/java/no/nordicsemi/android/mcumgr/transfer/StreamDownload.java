package no.nordicsemi.android.mcumgr.transfer;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;

import no.nordicsemi.android.mcumgr.McuMgrErrorCode;
import no.nordicsemi.android.mcumgr.exception.McuMgrErrorException;
import no.nordicsemi.android.mcumgr.exception.McuMgrException;
import no.nordicsemi.android.mcumgr.response.DownloadResponse;
import no.nordicsemi.android.mcumgr.response.McuMgrResponse;

@SuppressWarnings("unused")
public abstract class StreamDownload extends StreamTransfer {

    @NotNull
    private final OutputStream mDataOutput;

    @Nullable
    private final StreamDownloadCallback mCallback;

    protected StreamDownload(@NotNull OutputStream dataOutput) {
        this(dataOutput, null);
    }

    protected StreamDownload(
            @NotNull OutputStream dataOutput,
            @Nullable StreamDownloadCallback callback
    ) {
        mDataOutput = dataOutput;
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
            mDataLength = response.len;
        }

        // Validate response body
        if (response.data == null) {
            throw new McuMgrException("Download response data is null.");
        }
        if (mDataLength < 0) {
            throw new McuMgrException("Download size not set.");
        }

        try {
            mDataOutput.write(response.data);
        } catch (IOException e) {
            throw new McuMgrException("Download data failed to write to stream.", e);
        }
        mOffset = response.off + response.data.length;

        return response;
    }

    @Override
    public void reset() {
        mOffset = 0;
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
            mCallback.onDownloadCompleted();
        }
    }

    @Override
    public void onCanceled() {
        if (mCallback != null) {
            mCallback.onDownloadCanceled();
        }
    }
}
