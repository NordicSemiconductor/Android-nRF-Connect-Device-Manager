package no.nordicsemi.android.mcumgr.dfu.mcuboot.task;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nordicsemi.android.mcumgr.McuMgrTransport;
import no.nordicsemi.android.mcumgr.dfu.mcuboot.FirmwareUpgradeManager.Settings;
import no.nordicsemi.android.mcumgr.dfu.mcuboot.FirmwareUpgradeManager.State;
import no.nordicsemi.android.mcumgr.exception.McuMgrException;
import no.nordicsemi.android.mcumgr.task.TaskManager;

class Scan extends FirmwareUpgradeTask {
	private final static Logger LOG = LoggerFactory.getLogger(Scan.class);

	@NotNull
	private final String mAdvName;

	Scan(final @NotNull String advName) {
		this.mAdvName = advName;
	}

	@Override
	@NotNull
	public State getState() {
		return State.RESET;
	}

	@Override
	public int getPriority() {
		return PRIORITY_RESET_INITIAL;
	}

	@Override
	public void start(@NotNull final TaskManager<Settings, State> performer) {
		performer.getTransport().changeMode(mAdvName, new McuMgrTransport.ModeChangeCallback() {
			@Override
			public void onModeChanged() {
				LOG.info("Device switched to Firmware Loader mode");
				performer.onTaskCompleted(Scan.this);
			}

			@Override
			public void onError(@NotNull Throwable t) {
                LOG.error("Failed to switch device to Firmware Loader mode: {}", t.getMessage());
				if (t instanceof McuMgrException) {
					performer.onTaskFailed(Scan.this, (McuMgrException) t);
				} else {
					performer.onTaskFailed(Scan.this, new McuMgrException(t));
				}
			}
		});
	}
}
