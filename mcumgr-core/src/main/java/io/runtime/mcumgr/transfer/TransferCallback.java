package io.runtime.mcumgr.transfer;

import io.runtime.mcumgr.exception.McuMgrException;

public interface TransferCallback {
    /**
     * Called when a response has been received successfully.
     *
     * @param current   the number of bytes downloaded so far.
     * @param total     the total size of the download in bytes.
     * @param timestamp the timestamp of when the response was received.
     */
    void onProgressChanged(int current, int total, long timestamp);
    void onFailed(McuMgrException e);
    void onCompleted();
    void onCanceled();
}
