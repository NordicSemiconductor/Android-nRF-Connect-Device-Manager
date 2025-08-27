package no.nordicsemi.android.mcumgr.transfer;

import org.jetbrains.annotations.NotNull;

import no.nordicsemi.android.mcumgr.exception.McuMgrException;

public interface UploadCallback {
    /**
     * Called when a response has been received successfully.
     *
     * @param current the number of bytes sent so far.
     * @param total the size of the image in bytes.
     * @param timestamp the timestamp of when the response was received.
     */
    void onUploadProgressChanged(int current, int total, long timestamp);

    /**
     * Called when the upload has failed.
     *
     * @param error the error. See the cause for more info.
     */
    void onUploadFailed(@NotNull McuMgrException error);

    /**
     * Called when the upload has been canceled.
     */
    void onUploadCanceled();

    /**
     * Called when the upload has finished successfully.
     */
    void onUploadCompleted();
}
