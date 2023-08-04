package io.runtime.mcumgr.dfu.task;

import org.jetbrains.annotations.NotNull;

import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager.Settings;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager.State;
import io.runtime.mcumgr.dfu.model.McuMgrImageSet;
import io.runtime.mcumgr.task.TaskManager;

/**
 * This task performs the DFU. Given images will be sent to the target device, and tested and
 * confirmed, depending on the given mode.
 */
public class PerformDfu extends FirmwareUpgradeTask {

	@NotNull
	private final McuMgrImageSet images;
	@NotNull
	private final FirmwareUpgradeManager.Mode mode;

	private final boolean eraseSettings;

	public PerformDfu(final @NotNull FirmwareUpgradeManager.Mode mode,
					  final @NotNull McuMgrImageSet images,
					  final boolean eraseSettings) {
		this.mode = mode;
		this.images = images;
		this.eraseSettings = eraseSettings;
	}

	@NotNull
	@Override
	public State getState() {
		return State.VALIDATE;
	}

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public void start(final @NotNull TaskManager<Settings, State> performer) {
		performer.enqueue(new Validate(mode, images, eraseSettings));
		performer.onTaskCompleted(this);
	}
}
