package io.runtime.mcumgr.dfu.suit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.dfu.FirmwareUpgradeCallback;
import io.runtime.mcumgr.dfu.FirmwareUpgradeSettings;
import io.runtime.mcumgr.dfu.suit.task.PerformDfu;
import io.runtime.mcumgr.dfu.suit.task.SUITUpgradeTask;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.task.Task;
import io.runtime.mcumgr.task.TaskPerformer;

public class SUITUpgradePerformer extends TaskPerformer<SUITUpgradePerformer.Settings, SUITUpgradeManager.State> {
    private final static Logger LOG = LoggerFactory.getLogger(SUITUpgradePerformer.class);

    public static class Settings {
        @NotNull
        public FirmwareUpgradeSettings settings;
        @Nullable
        public SUITUpgradeManager.OnResourceRequiredCallback resourceCallback;

        public Settings(
                @NotNull final FirmwareUpgradeSettings settings,
                @Nullable final SUITUpgradeManager.OnResourceRequiredCallback resourceCallback
        ) {
            this.settings = settings;
            this.resourceCallback = resourceCallback;
        }
    }

    /**
     * Firmware upgrade callback passed into the constructor or set before the upload has started.
     */
    @NotNull
    private final FirmwareUpgradeCallback<SUITUpgradeManager.State> callback;

    SUITUpgradePerformer(@NotNull final FirmwareUpgradeCallback<SUITUpgradeManager.State> callback) {
        this.callback = callback;
    }

    SUITUpgradeManager.State getState() {
        final SUITUpgradeTask task = (SUITUpgradeTask) getCurrentTask();
        if (task == null)
            return SUITUpgradeManager.State.NONE;
        return task.getState();
    }

    void start(@NotNull final McuMgrTransport transport,
               @NotNull final Settings settings,
               final byte @NotNull [] envelope) {
        LOG.trace("Starting SUIT upgrade");
        super.start(transport, settings, new PerformDfu(envelope));
    }

    @Override
    public void onTaskStarted(final @Nullable Task<Settings, SUITUpgradeManager.State> previousTask,
                              final @NotNull Task<Settings, SUITUpgradeManager.State> nextTask) {
        if (previousTask == null)
            return;

        // Notify observer about changing the state.
        final SUITUpgradeManager.State oldState = previousTask.getState();
        final SUITUpgradeManager.State newState = nextTask.getState();
        if (oldState != null && newState != null && newState != oldState) {
            LOG.trace("Moving from state {} to state {}", oldState.name(), newState.name());
            callback.onStateChanged(oldState, newState);
        }
    }

    @Override
    public void onCompleted(final @NotNull Task<Settings, SUITUpgradeManager.State> task) {
        callback.onUpgradeCompleted();
    }

    @Override
    public void onTaskProgressChanged(final @NotNull Task<Settings, SUITUpgradeManager.State> task,
                                      final int current, final int total, final long timestamp) {
        callback.onUploadProgressChanged(current, total, timestamp);
    }

    @Override
    public void onCancelled(final @NotNull Task<Settings, SUITUpgradeManager.State> task) {
        callback.onUpgradeCanceled(task.getState());
    }

    @Override
    public void onTaskFailed(final @NotNull Task<Settings, SUITUpgradeManager.State> task,
                             final @NotNull McuMgrException error) {
        callback.onUpgradeFailed(task.getState(), error);
    }
}
