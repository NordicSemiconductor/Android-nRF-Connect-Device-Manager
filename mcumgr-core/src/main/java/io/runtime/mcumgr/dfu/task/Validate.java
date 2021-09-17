package io.runtime.mcumgr.dfu.task;

import android.util.Pair;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager.Mode;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager.State;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager.Settings;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.image.McuMgrImage;
import io.runtime.mcumgr.managers.ImageManager;
import io.runtime.mcumgr.response.img.McuMgrImageStateResponse;
import io.runtime.mcumgr.task.TaskManager;

class Validate extends FirmwareUpgradeTask {
	private final static Logger LOG = LoggerFactory.getLogger(Validate.class);

	private static final int SLOT_PRIMARY = 0;
	private static final int SLOT_SECONDARY = 1;

	@NotNull
	private final List<Pair<Integer, McuMgrImage>> images;
	@NotNull
	private final Mode mode;

	Validate(final @NotNull Mode mode,
			 final @NotNull List<Pair<Integer, McuMgrImage>> images) {
		this.mode = mode;
		this.images = images;
	}

	@Override
	@NotNull
	public State getState() {
		return State.VALIDATE;
	}

	@Override
	public int getPriority() {
		return PRIORITY_VALIDATE;
	}

	@Override
	public void start(@NotNull final TaskManager<Settings, State> performer) {
		final Settings settings = performer.getSettings();
		final ImageManager manager = new ImageManager(settings.transport);
		manager.list(new McuMgrCallback<McuMgrImageStateResponse>() {
			@Override
			public void onResponse(@NotNull final McuMgrImageStateResponse response) {
				LOG.trace("Validation response: {}", response.toString());

				// Check for an error return code.
				if (!response.isSuccess()) {
					performer.onTaskFailed(Validate.this, new McuMgrErrorException(response.getReturnCode()));
					return;
				}

				// Initial validation.
				McuMgrImageStateResponse.ImageSlot[] slots = response.images;
				if (slots == null) {
					LOG.error("Missing images information: {}", response.toString());
					performer.onTaskFailed(Validate.this, new McuMgrException("Missing images information"));
					return;
				}

				// The following code adds Erase, Upload, Test, Reset and Confirm operations
				// to the task priority queue. The priorities of those tasks ensure they are executed
				// in the right (given 2 lines above) order.

				// The flag indicates whether a reset operation should be performed during the process.
				boolean resetRequired = false;
				boolean initialResetRequired = false;

				// For each image that is to be sent, check if the same image has already been sent.
				for (final Pair<Integer, McuMgrImage> pair : images) {
					final int image = pair.first;
					final McuMgrImage mcuMgrImage = pair.second;

					// The following flags will be updated based on the received slot information.
					boolean found = false;
					boolean pending = false;   // TEST command was sent
					boolean permanent = false; // CONFIRM command was sent
					boolean confirmed = false; // Image has booted and confirmed itself
					for (final McuMgrImageStateResponse.ImageSlot slot : slots) {
						if (slot.image != image)
							continue;

						// If the same image was found in any of the slots, the upload will not be
						// required. The image may need testing or confirming, or may already be running.
						if (Arrays.equals(slot.hash, mcuMgrImage.getHash())) {
							found = true;
							pending = slot.pending;
							permanent = slot.permanent;
							confirmed = slot.confirmed;

							// If the image has been found on the secondary slot and it's confirmed,
							// we just need to restart the device in order for it to be swapped back to
							// primary slot.
							if (confirmed && slot.slot == SLOT_SECONDARY) {
								resetRequired = true;
							}
							break;
						} else {
							if (slot.slot == SLOT_SECONDARY) {
								// A different image in the secondary slot of required image may be found
								// in 3 cases:

								// 1. All flags are clear -> a previous update has taken place.
								//    The slot will be overridden automatically. Nothing needs to be done.
								if (!slot.pending && !slot.confirmed) {
									continue;
								}

								// 2. The confirmed flag is set -> the device is in test mode.
								//    In that case we need to reset the device to restore the original
								//    image. We could also confirm the image-under-test, but that's more
								//    risky.
								if (slot.confirmed) {
									initialResetRequired = true;
								}

								// 3. The pending or permanent flags are set -> the test or confirm
								//    command have been sent before, but reset was not performed.
								//    In that case we have to reset before uploading, as pending
								//    slot cannot be overwritten (NO MEMORY error would be returned).
								if (slot.pending || slot.permanent) {
									initialResetRequired = true;
								}
							}
						}
					}
					if (!found) {
						performer.enqueue(new Upload(mcuMgrImage, image));
					}
					switch (mode) {
						case TEST_AND_CONFIRM:
							// If the image is not pending (test command has not been sent) and not
							// confirmed (another image is under test), send test command and update the
							// flag.
							if (!pending && !confirmed) {
								performer.enqueue(new Test(mcuMgrImage.getHash()));
								pending = true;
							}
							// If the image is pending, reset is required.
							if (pending) {
								resetRequired = true;
							}
							if (!permanent && !confirmed) {
								performer.enqueue(new ConfirmAfterReset(mcuMgrImage.getHash()));
							}
							break;
						case TEST_ONLY:
							// If the image is not pending (test command has not been sent) and not
							// confirmed (another image is under test), send test command and update the
							// flag.
							if (!pending && !confirmed) {
								performer.enqueue(new Test(mcuMgrImage.getHash()));
								pending = true;
							}
							// If the image is pending, reset is required.
							if (pending) {
								resetRequired = true;
							}
							break;
						case CONFIRM_ONLY:
							// If the firmware is not confirmed yet, confirm t.
							if (!permanent && !confirmed) {
								performer.enqueue(new Confirm(mcuMgrImage.getHash()));
								permanent = true;
							}
							if (permanent) {
								resetRequired = true;
							}
							break;
					}
				}
				// To make sure the reset command are added just once, they're added based on flags.
				if (initialResetRequired) {
					performer.enqueue(new ResetBeforeUpload());
				}
				if (resetRequired) {
					performer.enqueue(new Reset());
				}

				performer.onTaskCompleted(Validate.this);
			}

			@Override
			public void onError(@NotNull final McuMgrException e) {
				performer.onTaskFailed(Validate.this, e);
			}
		});
	}
}
