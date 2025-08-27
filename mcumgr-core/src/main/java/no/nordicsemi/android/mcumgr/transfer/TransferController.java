package no.nordicsemi.android.mcumgr.transfer;

public interface TransferController {

    /**
     * Pause the transfer.
     */
    void pause();

    /**
     * Resume a paused transfer.
     */
    void resume();

    /**
     * Cancel the transfer.
     */
    void cancel();
}
