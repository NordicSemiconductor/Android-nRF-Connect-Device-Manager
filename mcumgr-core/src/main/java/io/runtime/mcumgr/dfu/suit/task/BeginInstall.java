package io.runtime.mcumgr.dfu.suit.task;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.dfu.suit.SUITUpgradeManager;
import io.runtime.mcumgr.dfu.suit.SUITUpgradePerformer;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.managers.SUITManager;
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.task.TaskManager;

class BeginInstall extends SUITUpgradeTask {
    private final static Logger LOG = LoggerFactory.getLogger(BeginInstall.class);

    @Override
    public int getPriority() {
        return PRIORITY_PROCESS;
    }

    @Override
    public @Nullable SUITUpgradeManager.State getState() {
        return SUITUpgradeManager.State.UPLOADING_RESOURCE;
    }

    @Override
    public void start(@NotNull TaskManager<SUITUpgradePerformer.Settings, SUITUpgradeManager.State> performer) {
        LOG.trace("Starting deferred install");

        final SUITManager manager = new SUITManager(performer.getTransport());
        manager.beginDeferredInstall(new McuMgrCallback<>() {
            @Override
            public void onResponse(@NotNull McuMgrResponse response) {
                performer.onTaskCompleted(BeginInstall.this);
            }

            @Override
            public void onError(@NotNull McuMgrException error) {
                performer.onTaskFailed(BeginInstall.this, error);
            }
        });
    }
}
