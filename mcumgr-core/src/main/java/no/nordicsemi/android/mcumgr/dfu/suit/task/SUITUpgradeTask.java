package no.nordicsemi.android.mcumgr.dfu.suit.task;

import no.nordicsemi.android.mcumgr.dfu.suit.SUITUpgradeManager;
import no.nordicsemi.android.mcumgr.dfu.suit.SUITUpgradePerformer;
import no.nordicsemi.android.mcumgr.task.Task;

public abstract class SUITUpgradeTask extends Task<SUITUpgradePerformer.Settings, SUITUpgradeManager.State> {
    final static int PRIORITY_UPLOAD = 0;
    final static int PRIORITY_PROCESS = 1;
}
