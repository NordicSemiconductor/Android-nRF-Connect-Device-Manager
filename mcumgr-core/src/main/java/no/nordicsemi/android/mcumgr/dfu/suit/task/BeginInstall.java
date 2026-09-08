package no.nordicsemi.android.mcumgr.dfu.suit.task;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import no.nordicsemi.android.mcumgr.McuMgrCallback;
import no.nordicsemi.android.mcumgr.dfu.suit.SUITUpgradeManager;
import no.nordicsemi.android.mcumgr.dfu.suit.SUITUpgradePerformer;
import no.nordicsemi.android.mcumgr.exception.McuMgrException;
import no.nordicsemi.android.mcumgr.log.McuMgrLogger;
import no.nordicsemi.android.mcumgr.managers.SUITManager;
import no.nordicsemi.android.mcumgr.response.McuMgrResponse;
import no.nordicsemi.android.mcumgr.task.TaskManager;

class BeginInstall extends SUITUpgradeTask {
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
        final McuMgrLogger log = performer.getLog();

        final SUITManager manager = new SUITManager(performer.getTransport());
        manager.setLogger(log.getSink());
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
