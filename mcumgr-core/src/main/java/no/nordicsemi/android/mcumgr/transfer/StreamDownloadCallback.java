package no.nordicsemi.android.mcumgr.transfer;

import org.jetbrains.annotations.NotNull;

import no.nordicsemi.android.mcumgr.exception.McuMgrException;

public interface StreamDownloadCallback {
    /**
     * Called when a response has been received successfully.
     *
     * @param current   the number of bytes downloaded so far.
     * @param total     the total size of the download in bytes.
     * @param timestamp the timestamp of when the response was received.
     */
    void onDownloadProgressChanged(int current, int total, long timestamp);

    /**
     * Called when the download has failed.
     *
     * @param error the error. See the cause for more info.
     */
    void onDownloadFailed(@NotNull McuMgrException error);

    /**
     * Called when the download has been canceled.
     */
    void onDownloadCanceled();

    /**
     * Called when the download has finished successfully.
     */
    void onDownloadCompleted();
}
