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

    /**
     * Upload controller used to pause, resume, and cancel upload. Set when the upload is started.
     */
    private TransferController mUploadController;

    public UploadEnvelope(final byte @NotNull [] envelope) {
        this.envelope = envelope;
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

        // After the upload is complete, client should poll for more resources.
        performer.enqueue(new PollTask());

        final UploadCallback callback = new UploadCallback() {
            @Override
            public void onUploadProgressChanged(final int current, final int total, final long timestamp) {
                performer.onTaskProgressChanged(UploadEnvelope.this, current, total, timestamp);
            }

            @Override
            public void onUploadFailed(@NotNull final McuMgrException error) {
                performer.onTaskFailed(UploadEnvelope.this, error);
            }

            @Override
            public void onUploadCanceled() {
                performer.onTaskCompleted(UploadEnvelope.this);
            }

            @Override
            public void onUploadCompleted() {
                performer.onTaskCompleted(UploadEnvelope.this);
            }
        };

        LOG.info("Uploading SUIT envelope of size: {}", envelope.length);
        final SUITUpgradePerformer.Settings settings = performer.getSettings();
        final SUITManager manager = new SUITManager(performer.getTransport());
        mUploadController =	new EnvelopeUploader(
                manager,
                envelope,
                settings.settings.windowCapacity,
                settings.settings.memoryAlignment
        ).uploadAsync(callback);
    }

    @Override
    public void pause() {
        mUploadController.pause();
    }

    @Override
    public void cancel() {
        mUploadController.cancel();
    }
}
