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
import io.runtime.mcumgr.transfer.CacheUploader;
import io.runtime.mcumgr.transfer.TransferController;
import io.runtime.mcumgr.transfer.UploadCallback;

class UploadCache extends SUITUpgradeTask {
    private final static Logger LOG = LoggerFactory.getLogger(UploadCache.class);

    private final byte @NotNull [] data;
    private final int targetId;
    private boolean canceled = false;

    /**
     * Upload controller used to pause, resume, and cancel upload. Set when the upload is started.
     */
    private TransferController mUploadController;

    public UploadCache(
            final int targetId,
            final byte @NotNull [] data
    ) {
        this.targetId = targetId;
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
        // Should we resume?
        if (mUploadController != null) {
            mUploadController.resume();
            return;
        }

        final UploadCallback callback = new UploadCallback() {
            @Override
            public void onUploadProgressChanged(final int current, final int total, final long timestamp) {
                performer.onTaskProgressChanged(UploadCache.this, current, total, timestamp);
            }

            @Override
            public void onUploadFailed(@NotNull final McuMgrException error) {
                LOG.info("Upload failed: {}", error.getMessage());
                performer.onTaskFailed(UploadCache.this, error);
            }

            @Override
            public void onUploadCanceled() {
                LOG.info("Uploading cancelled");
                performer.onTaskCompleted(UploadCache.this);
            }

            @Override
            public void onUploadCompleted() {
                LOG.info("Uploading complete");
                performer.onTaskCompleted(UploadCache.this);
            }
        };

        // Check if the task was canceled before starting the upload.
        if (canceled) {
            callback.onUploadCanceled();
            return;
        }

        LOG.info("Uploading cache image with target partition ID: {} ({} bytes)", targetId, data.length);
        final SUITUpgradePerformer.Settings settings = performer.getSettings();
        final SUITManager manager = new SUITManager(performer.getTransport());
        mUploadController =	new CacheUploader(
                manager,
                targetId,
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
