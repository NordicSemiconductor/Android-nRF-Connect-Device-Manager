package io.runtime.mcumgr.dfu.suit.task;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.runtime.mcumgr.dfu.suit.SUITUpgradeManager;
import io.runtime.mcumgr.dfu.suit.SUITUpgradePerformer;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.managers.SUITManager;
import io.runtime.mcumgr.task.TaskManager;
import io.runtime.mcumgr.transfer.EnvelopeUploader;
import io.runtime.mcumgr.transfer.TransferController;
import io.runtime.mcumgr.transfer.UploadCallback;

class UploadEnvelope extends SUITUpgradeTask {
    private final static Logger LOG = LoggerFactory.getLogger(UploadEnvelope.class);
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
                LOG.info("Upload failed: {}", error.getMessage());
                performer.onTaskFailed(UploadEnvelope.this, error);
            }

            @Override
            public void onUploadCanceled() {
                LOG.info("Uploading canceled");
                performer.onTaskCompleted(UploadEnvelope.this);
            }

            @Override
            public void onUploadCompleted() {
                LOG.info("Uploading complete");
                performer.onTaskCompleted(UploadEnvelope.this);
            }
        };

        // Check if the task was canceled before starting the upload.
        if (canceled) {
            callback.onUploadCanceled();
            return;
        }

        LOG.info("Uploading SUIT envelope of size: {}", envelope.length);
        final SUITUpgradePerformer.Settings settings = performer.getSettings();
        final SUITManager manager = new SUITManager(performer.getTransport());
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
