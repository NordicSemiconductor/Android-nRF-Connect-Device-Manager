package no.nordicsemi.android.mcumgr.dfu.mcuboot.task;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import no.nordicsemi.android.mcumgr.McuMgrCallback;
import no.nordicsemi.android.mcumgr.dfu.mcuboot.FirmwareUpgradeManager.Settings;
import no.nordicsemi.android.mcumgr.dfu.mcuboot.FirmwareUpgradeManager.State;
import no.nordicsemi.android.mcumgr.exception.McuMgrErrorException;
import no.nordicsemi.android.mcumgr.exception.McuMgrException;
import no.nordicsemi.android.mcumgr.log.McuMgrLogger;
import no.nordicsemi.android.mcumgr.managers.ImageManager;
import no.nordicsemi.android.mcumgr.response.img.McuMgrImageStateResponse;
import no.nordicsemi.android.mcumgr.task.Task;
import no.nordicsemi.android.mcumgr.task.TaskManager;

class Test extends FirmwareUpgradeTask {
	private final byte @NotNull [] hash;
	private final int imageIndex;

	Test(final int imageIndex, final byte @NotNull [] hash) {
		this.imageIndex = imageIndex;
		this.hash = hash;
	}

	@Override
	@NotNull
	public State getState() {
		return State.TEST;
	}

	@Override
	public int getPriority() {
		return PRIORITY_TEST_AFTER_UPLOAD;
	}

	@Override
	public int compareTo(Task<Settings, State> o) {
		// Test operation should be sent from lowest to highest image index.
		if (o instanceof Test) {
			final Test test = (Test) o;
			return imageIndex - test.imageIndex;
		}
		return super.compareTo(o);
	}

	@Override
	public void start(final @NotNull TaskManager<Settings, State> performer) {
		final McuMgrLogger log = performer.getLog();

		final ImageManager manager = new ImageManager(performer.getTransport());
		manager.setLogger(log.getSink());
		manager.test(hash, new McuMgrCallback<>() {
			@Override
			public void onResponse(@NotNull final McuMgrImageStateResponse response) {
				log.trace("Test response: {}", response);
				// Check for an error return code.
				if (!response.isSuccess()) {
					performer.onTaskFailed(Test.this, new McuMgrErrorException(response.getReturnCode()));
					return;
				}
				// Search for tested slot and check its status.
				for (final McuMgrImageStateResponse.ImageSlot slot : response.images) {
					if (Arrays.equals(slot.hash, hash)) {
						if (slot.pending) {
							performer.onTaskCompleted(Test.this);
						} else {
							log.warn("Pending flag of slot {} of image {} not set. Possible reason: downgrade prevention enabled", slot.slot, slot.image);
							performer.onTaskFailed(Test.this, new McuMgrException("Downgrade forbidden"));
						}
						return;
					}
				}
				performer.onTaskFailed(Test.this, new McuMgrException("Tested image not found."));
			}

			@Override
			public void onError(@NotNull McuMgrException e) {
				performer.onTaskFailed(Test.this, e);
			}
		});
	}
}
