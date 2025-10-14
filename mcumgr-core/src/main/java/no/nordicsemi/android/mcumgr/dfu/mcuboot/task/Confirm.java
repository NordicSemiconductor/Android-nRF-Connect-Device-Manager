package no.nordicsemi.android.mcumgr.dfu.mcuboot.task;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

class Confirm extends FirmwareUpgradeTask {
	private final static Logger LOG = LoggerFactory.getLogger(Confirm.class);

	private final byte @Nullable [] hash;

	Confirm(final byte @NotNull [] hash) {
		this.hash = hash;
	}

	Confirm() {
		this.hash = null;
	}

	@Override
	@NotNull
	public State getState() {
		return State.CONFIRM;
	}

	@Override
	public int getPriority() {
		return PRIORITY_CONFIRM_AFTER_UPLOAD;
	}

	@Override
	public void start(final @NotNull TaskManager<Settings, State> performer) {
		final ImageManager manager = new ImageManager(performer.getTransport());
		manager.confirm(hash, new McuMgrCallback<>() {
			@Override
			public void onResponse(@NotNull final McuMgrImageStateResponse response) {
				LOG.trace("Confirm response: {}", response);
				// Check for an error return code.
				if (!response.isSuccess()) {
					performer.onTaskFailed(Confirm.this, new McuMgrErrorException(response.getReturnCode()));
					return;
				}

				// MCUboot returns the list of images in the response.
				final McuMgrImageStateResponse.ImageSlot[] images = response.images;
				if (images != null) {
					// Search for slot for which the confirm command was sent and check its status.
					for (final McuMgrImageStateResponse.ImageSlot slot : images) {
						if (Arrays.equals(slot.hash, hash)) {
							if (slot.permanent || slot.confirmed) {
								performer.onTaskCompleted(Confirm.this);
							} else {
								LOG.warn("Permanent flag of slot {} of image {} not set. Possible reason: downgrade prevention enabled", slot.slot, slot.image);
								performer.onTaskFailed(Confirm.this, new McuMgrException("Downgrade forbidden"));
							}
							return;
						}
					}
					performer.onTaskFailed(Confirm.this, new McuMgrException("Confirmed image not found"));
					return;
				}
				// SUIT implementation of Image manager does not return images from Confirm command.
				// Instead, the device will reset and the new image will be booted.
				performer.onTaskCompleted(Confirm.this);
			}

			@Override
			public void onError(@NotNull McuMgrException e) {
				performer.onTaskFailed(Confirm.this, e);
			}
		});
	}
}
