package io.runtime.mcumgr.dfu.task;

import io.runtime.mcumgr.dfu.FirmwareUpgradeManager.Settings;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager.State;
import io.runtime.mcumgr.task.Task;

public abstract class FirmwareUpgradeTask extends Task<Settings, State> {
	final static int PRIORITY_VALIDATE = 0;
	final static int PRIORITY_RESET_INITIAL = 1;
	final static int PRIORITY_UPLOAD = 2;
	final static int PRIORITY_TEST_AFTER_UPLOAD = PRIORITY_UPLOAD + 1;
	final static int PRIORITY_CONFIRM_AFTER_UPLOAD = PRIORITY_UPLOAD + 1;
	final static int PRIORITY_RESET = 10;
	final static int PRIORITY_CONFIRM_AFTER_RESET = PRIORITY_RESET + 1;
}
