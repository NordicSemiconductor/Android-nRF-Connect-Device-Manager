package no.nordicsemi.android.mcumgr.dfu.suit.task;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import no.nordicsemi.android.mcumgr.dfu.suit.SUITUpgradeManager;
import no.nordicsemi.android.mcumgr.dfu.suit.SUITUpgradePerformer;
import no.nordicsemi.android.mcumgr.exception.McuMgrException;
import no.nordicsemi.android.mcumgr.log.McuMgrLogger;
import no.nordicsemi.android.mcumgr.managers.SUITManager;
import no.nordicsemi.android.mcumgr.task.TaskManager;
import no.nordicsemi.android.mcumgr.transfer.EnvelopeUploader;
import no.nordicsemi.android.mcumgr.transfer.TransferController;
import no.nordicsemi.android.mcumgr.transfer.UploadCallback;

class UploadEnvelope extends SUITUpgradeTask {
    private final byte @NotNull [] envelope;
    private final boolean deferInstall;
    private boolean canceled = false;

    /**
     * Upload controller used to pause, resume, and cancel upload. Set when the upload is started.
     */
    @Nullable
    private TransferController mUploadController;

    public UploadEnvelope(final byte @NotNull [] envelope, final boolean deferInstall) {
        this.envelope = envelope;
        this.deferInstall = deferInstall;
    }

    @Override
    public int getPriority() {
        return PRIORITY_UPLOAD;
    }

    @Override
    public @Nullable SUITUpgradeManager.State getState() {
        return SUITUpgradeManager.State.UPLOADING_ENVELOPE;
    }

    @Override
    public void start(@NotNull TaskManager<SUITUpgradePerformer.Settings, SUITUpgradeManager.State> performer) {
        final McuMgrLogger log = performer.getLog();

        // Should we resume?
        if (mUploadController != null) {
            mUploadController.resume();
            return;
        }

        final UploadCallback callback = new UploadCallback() {
            @Override
            public void onUploadProgressChanged(final int current, final int total, final long timestamp) {
                performer.onTaskProgressChanged(UploadEnvelope.this, current, total, timestamp);
            }

            @Override
            public void onUploadFailed(@NotNull final McuMgrException error) {
                log.error("Upload failed: {}", error.getMessage());
                performer.onTaskFailed(UploadEnvelope.this, error);
            }

            @Override
            public void onUploadCanceled() {
                log.warn("Uploading canceled");
                performer.onTaskCompleted(UploadEnvelope.this);
            }

            @Override
            public void onUploadCompleted() {
                log.info("Uploading complete");
                performer.onTaskCompleted(UploadEnvelope.this);
            }
        };

        // Check if the task was canceled before starting the upload.
        if (canceled) {
            callback.onUploadCanceled();
            return;
        }

        log.info("Uploading SUIT envelope of size: {}", envelope.length);
        final SUITUpgradePerformer.Settings settings = performer.getSettings();
        final SUITManager manager = new SUITManager(performer.getTransport());
        manager.setLogger(log.getSink());
        mUploadController =	new EnvelopeUploader(
                manager,
                envelope,
                settings.settings.windowCapacity,
                settings.settings.memoryAlignment,
                deferInstall
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
