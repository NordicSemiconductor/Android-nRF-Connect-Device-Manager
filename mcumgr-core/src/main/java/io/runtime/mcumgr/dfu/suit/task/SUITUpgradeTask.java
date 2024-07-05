package io.runtime.mcumgr.dfu.suit.task;

import io.runtime.mcumgr.dfu.suit.SUITUpgradeManager;
import io.runtime.mcumgr.dfu.suit.SUITUpgradePerformer;
import io.runtime.mcumgr.task.Task;

public abstract class SUITUpgradeTask extends Task<SUITUpgradePerformer.Settings, SUITUpgradeManager.State> {
    final static int PRIORITY_UPLOAD = 0;
    final static int PRIORITY_PROCESS = 1;
}
