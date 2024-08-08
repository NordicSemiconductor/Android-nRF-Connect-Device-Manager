package io.runtime.mcumgr.transfer;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.runtime.mcumgr.McuManager;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.InsufficientMtuException;

public class TransferManager extends McuManager {

    private ExecutorService mExecutor;

    /**
     * Construct a McuManager instance.
     *
     * @param groupId     the group ID of this Mcu Manager instance.
     * @param transporter the transporter to use to send commands.
     */
    protected TransferManager(int groupId, @NotNull McuMgrTransport transporter) {
        super(groupId, transporter);
    }

    /**
     * Start an upload.
     * <p>
     * If there is an active transfer being executed on this manager, the transfer will be queued.
     *
     * @param upload The upload to start.
     * @return The controller used to pause, resume, or cancel the upload.
     */
    @NotNull
    public TransferController startUpload(@NotNull Upload upload) {
        return startTransfer(upload);
    }

    /**
     * Start an upload.
     * <p>
     * If there is an active transfer being executed on this manager, the transfer will be queued.
     *
     * @param upload The upload to start.
     * @return The controller used to pause, resume, or cancel the upload.
     */
    @NotNull
    public TransferController startUpload(@NotNull StreamUpload upload) {
        return startTransfer(upload);
    }

    /**
     * Start an download.
     * <p>
     * If there is an active transfer being executed on this manager, the download will be queued.
     *
     * @param download The upload to start.
     * @return The controller used to pause, resume, or cancel the download.
     */
    @NotNull
    public TransferController startDownload(@NotNull Download download) {
        return startTransfer(download);
    }

    /**
     * Start an download.
     * <p>
     * If there is an active transfer being executed on this manager, the download will be queued.
     *
     * @param download The upload to start.
     * @return The controller used to pause, resume, or cancel the download.
     */
    @NotNull
    public TransferController startDownload(@NotNull StreamDownload download) {
        return startTransfer(download);
    }

    @NotNull
    private synchronized TransferController startTransfer(@NotNull final Transfer transfer) {

        final TransferCallable transferCallable = new TransferCallable(transfer);

        /*
         * Wrap the callable in the in an runnable which catches InsufficientMtuException and
         * retries the transfer once.
         */
        getTransferExecutor().execute(new Runnable() {

            // Whether to retry with a new MTU due to an MTU exception.
            private boolean mRetry = true;

            @Override
            public void run() {
                try {
                    // Execute the transfer callable.
                    transferCallable.call();
                } catch (InsufficientMtuException e) {
                    // If we have already retried, fail the transfer.
                    if (!mRetry) {
                        transferCallable.getTransfer().onFailed(e);
                        return;
                    }
                    // Set the MTU to the value specified in the error response.
                    int mtu = e.getMtu();
                    if (mMtu == mtu) {
                        mtu -= 1;
                    }
                    boolean isMtuSet = setUploadMtu(mtu);
                    if (isMtuSet) {
                        // If the MTU has been set successfully, restart the upload.
                        transferCallable.getTransfer().reset();
                        mRetry = false;
                        run();
                    } else {
                        transferCallable.getTransfer().onFailed(e);
                    }
                }
            }
        });
        return transferCallable;
    }

    @NotNull
    private synchronized ExecutorService getTransferExecutor() {
        if (mExecutor == null) {
            mExecutor = Executors.newSingleThreadExecutor();
        }
        return mExecutor;
    }
}
