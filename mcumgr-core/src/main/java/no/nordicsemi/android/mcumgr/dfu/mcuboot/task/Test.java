package no.nordicsemi.android.mcumgr.dfu.mcuboot.task;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import no.nordicsemi.android.mcumgr.McuMgrCallback;
import no.nordicsemi.android.mcumgr.dfu.mcuboot.FirmwareUpgradeManager.Settings;
import no.nordicsemi.android.mcumgr.dfu.mcuboot.FirmwareUpgradeManager.State;
import no.nordicsemi.android.mcumgr.exception.McuMgrErrorException;
import no.nordicsemi.android.mcumgr.exception.McuMgrException;
import no.nordicsemi.android.mcumgr.managers.ImageManager;
import no.nordicsemi.android.mcumgr.response.img.McuMgrImageStateResponse;
import no.nordicsemi.android.mcumgr.task.TaskManager;

class Test extends FirmwareUpgradeTask {
	private final static Logger LOG = LoggerFactory.getLogger(Test.class);

	private final byte @NotNull [] hash;

	Test(final byte @NotNull [] hash) {
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
	public void start(final @NotNull TaskManager<Settings, State> performer) {
		final ImageManager manager = new ImageManager(performer.getTransport());
		manager.test(hash, new McuMgrCallback<>() {
			@Override
			public void onResponse(@NotNull final McuMgrImageStateResponse response) {
				LOG.trace("Test response: {}", response);
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
							performer.onTaskFailed(Test.this, new McuMgrException("Tested image is not in a pending state."));
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
