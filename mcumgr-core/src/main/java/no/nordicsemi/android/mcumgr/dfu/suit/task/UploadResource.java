package no.nordicsemi.android.mcumgr.dfu.suit.task;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import no.nordicsemi.android.mcumgr.dfu.suit.SUITUpgradeManager;
import no.nordicsemi.android.mcumgr.dfu.suit.SUITUpgradePerformer;
import no.nordicsemi.android.mcumgr.exception.McuMgrException;
import no.nordicsemi.android.mcumgr.log.McuMgrLogger;
import no.nordicsemi.android.mcumgr.managers.SUITManager;
import no.nordicsemi.android.mcumgr.task.TaskManager;
import no.nordicsemi.android.mcumgr.transfer.ResourceUploader;
import no.nordicsemi.android.mcumgr.transfer.TransferController;
import no.nordicsemi.android.mcumgr.transfer.UploadCallback;

class UploadResource extends SUITUpgradeTask {
    private final byte @NotNull [] data;
    private final int sessionId;
    private boolean canceled = false;

    /**
     * Upload controller used to pause, resume, and cancel upload. Set when the upload is started.
     */
    @Nullable
    private TransferController mUploadController;

    public UploadResource(
            final int sessionId,
            final byte @NotNull [] data
    ) {
        this.sessionId = sessionId;
        this.data = data;
    }

    @Override
    public int getPriority() {
        return PRIORITY_UPLOAD;
    }

    @Override
    public @Nullable SUITUpgradeManager.State getState() {
        return SUITUpgradeManager.State.UPLOADING_RESOURCE;
    }

    @Override
    public void start(@NotNull TaskManager<SUITUpgradePerformer.Settings, SUITUpgradeManager.State> performer) {
        final McuMgrLogger log = performer.getLog();

        // Should we resume?
        if (mUploadController != null) {
            mUploadController.resume();
            return;
        }

        // After the upload is complete, client should poll for more resources.
        performer.enqueue(new PollTask());

        final UploadCallback callback = new UploadCallback() {
            @Override
            public void onUploadProgressChanged(final int current, final int total, final long timestamp) {
                performer.onTaskProgressChanged(UploadResource.this, current, total, timestamp);
            }

            @Override
            public void onUploadFailed(@NotNull final McuMgrException error) {
                log.error("Upload failed: {}", error.getMessage());
                performer.onTaskFailed(UploadResource.this, error);
            }

            @Override
            public void onUploadCanceled() {
                log.warn("Uploading cancelled");
                performer.onTaskCompleted(UploadResource.this);
            }

            @Override
            public void onUploadCompleted() {
                log.info("Uploading complete");
                performer.onTaskCompleted(UploadResource.this);
            }
        };

        // Check if the task was canceled before starting the upload.
        if (canceled) {
            callback.onUploadCanceled();
            return;
        }

        log.info("Uploading resource with session ID: {} ({} bytes)", sessionId, data.length);
        final SUITUpgradePerformer.Settings settings = performer.getSettings();
        final SUITManager manager = new SUITManager(performer.getTransport());
        manager.setLogger(log.getSink());
        mUploadController =	new ResourceUploader(
                manager,
                sessionId,
                data,
                settings.settings.windowCapacity,
                settings.settings.memoryAlignment
        ).uploadAsync(callback);
    }

    @Override
    public void pause() {
        if (mUploadController != null) {
            mUploadController.pause();
        }
    }

    @Override
    public void cancel() {
        if (mUploadController != null) {
            mUploadController.cancel();
        } else {
            canceled = true;
        }
    }
}
