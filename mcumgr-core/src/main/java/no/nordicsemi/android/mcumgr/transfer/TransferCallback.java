package no.nordicsemi.android.mcumgr.transfer;

import org.jetbrains.annotations.NotNull;

import no.nordicsemi.android.mcumgr.exception.McuMgrException;

public interface TransferCallback {
    /**
     * Called when a response has been received successfully.
     *
     * @param current   the number of bytes downloaded so far.
     * @param total     the total size of the download in bytes.
     * @param timestamp the timestamp of when the response was received.
     */
    void onProgressChanged(int current, int total, long timestamp);

    /**
     * Called when a response with failure has been received.
     *
     * @param e the exception.
     */
    void onFailed(@NotNull McuMgrException e);

    /**
     * Called when the transfer is complete.
     */
    void onCompleted();

    /**
     * Called when the transfer has been cancelled.
     */
    void onCanceled();
}
