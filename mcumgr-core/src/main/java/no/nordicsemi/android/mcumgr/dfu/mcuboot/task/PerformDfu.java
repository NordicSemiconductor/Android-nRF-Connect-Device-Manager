package no.nordicsemi.android.mcumgr.dfu.mcuboot.task;

import org.jetbrains.annotations.NotNull;

import no.nordicsemi.android.mcumgr.dfu.mcuboot.FirmwareUpgradeManager;
import no.nordicsemi.android.mcumgr.dfu.mcuboot.FirmwareUpgradeManager.Settings;
import no.nordicsemi.android.mcumgr.dfu.mcuboot.FirmwareUpgradeManager.State;
import no.nordicsemi.android.mcumgr.dfu.mcuboot.model.ImageSet;
import no.nordicsemi.android.mcumgr.task.TaskManager;

/**
 * This task performs the DFU. Given images will be sent to the target device, and tested and
 * confirmed, depending on the given mode.
 */
public class PerformDfu extends FirmwareUpgradeTask {

	@NotNull
	private final ImageSet images;
	@NotNull
	private final FirmwareUpgradeManager.Mode mode;

	public PerformDfu(final @NotNull FirmwareUpgradeManager.Mode mode,
					  final @NotNull ImageSet images) {
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
