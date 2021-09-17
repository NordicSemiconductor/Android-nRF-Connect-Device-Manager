package io.runtime.mcumgr.dfu.task;

import android.util.Pair;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager.State;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager.Settings;
import io.runtime.mcumgr.image.McuMgrImage;
import io.runtime.mcumgr.task.TaskManager;

/**
 * This task performs the DFU. Given images will be sent to the target device, and tested and
 * confirmed, depending on the given mode.
 */
public class PerformDfu extends FirmwareUpgradeTask {

	@NotNull
	private final List<Pair<Integer, McuMgrImage>> images;
	@NotNull
	private final FirmwareUpgradeManager.Mode mode;

	public PerformDfu(final @NotNull FirmwareUpgradeManager.Mode mode,
					  final @NotNull List<Pair<Integer, McuMgrImage>> images) {
		this.mode = mode;
		this.images = images;
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
		performer.enqueue(new Validate(mode, images));
		performer.onTaskCompleted(this);
	}
}
