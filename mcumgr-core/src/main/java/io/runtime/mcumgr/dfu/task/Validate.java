package io.runtime.mcumgr.dfu.task;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager.Mode;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager.Settings;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager.State;
import io.runtime.mcumgr.dfu.model.McuMgrImageSet;
import io.runtime.mcumgr.dfu.model.McuMgrTargetImage;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.image.McuMgrImage;
import io.runtime.mcumgr.managers.DefaultManager;
import io.runtime.mcumgr.managers.ImageManager;
import io.runtime.mcumgr.response.dflt.McuMgrBootloaderInfoResponse;
import io.runtime.mcumgr.response.img.McuMgrImageStateResponse;
import io.runtime.mcumgr.task.TaskManager;

class Validate extends FirmwareUpgradeTask {
	private final static Logger LOG = LoggerFactory.getLogger(Validate.class);

	@NotNull
	private final McuMgrImageSet images;
	@NotNull
	private final Mode mode;

	private final boolean eraseSettings;

	Validate(final @NotNull Mode mode,
			 final @NotNull McuMgrImageSet images,
			 final boolean eraseSettings) {
		this.mode = mode;
		this.images = images;
		this.eraseSettings = eraseSettings;
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
		final DefaultManager manager = new DefaultManager(settings.transport);

		// Starting from NCS 2.5 different bootloader modes allow sending the image in
		// slightly different ways. For that, we need to read bootloader info.
		// If that command is not supported, we assume the old, normal way of sending.
		manager.bootloaderInfo(DefaultManager.BOOTLOADER_INFO_QUERY_BOOTLOADER, new McuMgrCallback<>() {
			@Override
			public void onResponse(@NotNull final McuMgrBootloaderInfoResponse response) {
				LOG.trace("Bootloader name: {}", response.bootloader);

				if ("MCUboot".equals(response.bootloader)) {
					manager.bootloaderInfo(DefaultManager.BOOTLOADER_INFO_MCUBOOT_QUERY_MODE, new McuMgrCallback<>() {
						@Override
						public void onResponse(@NotNull McuMgrBootloaderInfoResponse response) {
							LOG.info("Bootloader is in mode: {}, no downgrade: {}", parseMode(response.mode), response.noDowngrade);
							validate(performer,
									response.mode == McuMgrBootloaderInfoResponse.MODE_DIRECT_XIP || response.mode == McuMgrBootloaderInfoResponse.MODE_DIRECT_XIP_WITH_REVERT,
									response.mode != McuMgrBootloaderInfoResponse.MODE_DIRECT_XIP);
						}

						@Override
						public void onError(@NotNull McuMgrException error) {
							// Pretend nothing happened.
							validate(performer, false, true);
						}
					});
				} else {
					// It's some unknown bootloader. Try sending the old way.
					validate(performer, false, true);
				}
			}

			@Override
			public void onError(@NotNull final McuMgrException error) {
				// Pretend nothing happened.
				validate(performer, false, true);
			}
		});
	}

	private void validate(@NotNull final TaskManager<Settings, State> performer,
						  final boolean noSwap,
						  final boolean allowRevert) {
		final Settings settings = performer.getSettings();
		final ImageManager manager = new ImageManager(settings.transport);

		manager.list(new McuMgrCallback<>() {
			@Override
			public void onResponse(@NotNull final McuMgrImageStateResponse response) {
				LOG.trace("Validation response: {}", response);

				// Check for an error return code.
				if (!response.isSuccess()) {
					performer.onTaskFailed(Validate.this, new McuMgrErrorException(response.getReturnCode()));
					return;
				}

				// Initial validation.
				McuMgrImageStateResponse.ImageSlot[] slots = response.images;
				if (slots == null) {
					LOG.error("Missing images information: {}", response);
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
				for (final McuMgrTargetImage image : images.getImages()) {
					final int imageIndex = image.imageIndex;
					final McuMgrImage mcuMgrImage = image.image;

					// The following flags will be updated based on the received slot information.
					boolean found = false;     // An image with the same hash was found on the device
					boolean skip = false;      // When this flag is set the image will not be uploaded
					boolean pending = false;   // TEST command was sent
					boolean permanent = false; // CONFIRM command was sent
					boolean confirmed = false; // Image has booted and confirmed itself
					boolean active = false;    // Image is currently running
					for (final McuMgrImageStateResponse.ImageSlot slot : slots) {
						// Skip slots of a different core than the image is for.
						if (slot.image != imageIndex)
							continue;

						// If the same image was found in any of the slots, the upload will not be
						// required. The image may need testing or confirming, or may already be running.
						if (Arrays.equals(slot.hash, mcuMgrImage.getHash())) {
							found = true;
							pending = slot.pending;
							permanent = slot.permanent;
							confirmed = slot.confirmed;
							active = slot.active;

							// If the image has been found on its target slot and it's confirmed,
							// we just need to restart the device in order for it to be swapped back to
							// primary slot.
							if (confirmed && slot.slot == image.slot && !noSwap) {
								resetRequired = true;
							}
							break;
						} else {
							if (slot.slot == image.slot) {
								// The `image.slot` determines to which slot the image will be uploaded.
								// If the image on the target slot is active, we cannot send there
								// anything, so we skip this image. It will not be uploaded.
								// This can happen on the primary slot or, when Direct XIP feature
								// is enabled, also on the secondary slot.
								if (slot.active) {
									skip = true;
									continue;
								}
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
					if (skip) {
						continue;
					}
					if (!found) {
						performer.enqueue(new Upload(mcuMgrImage, imageIndex));
						if (!allowRevert || mode == Mode.NONE) {
							resetRequired = true;
						}
					}
					if (allowRevert && mode != Mode.NONE) {
						switch (mode) {
							case TEST_AND_CONFIRM -> {
								// If the image is not pending (test command has not been sent) and not
								// confirmed (another image is under test), and isn't the currently
								// running image, send test command and update the flag.
								if (!pending && !confirmed && !active) {
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
							}
							case TEST_ONLY -> {
								// If the image is not pending (test command has not been sent) and not
								// confirmed (another image is under test), and isn't the currently
								// running image, send test command and update the flag.
								if (!pending && !confirmed && !active) {
									performer.enqueue(new Test(mcuMgrImage.getHash()));
									pending = true;
								}
								// If the image is pending, reset is required.
								if (pending) {
									resetRequired = true;
								}
							}
							case CONFIRM_ONLY -> {
								// If the firmware is not confirmed yet, confirm t.
								if (!permanent && !confirmed) {
									performer.enqueue(new Confirm(mcuMgrImage.getHash()));
									permanent = true;
								}
								if (permanent) {
									resetRequired = true;
								}
							}
						}
					}
				}
				// To make sure the reset command are added just once, they're added based on flags.
				if (initialResetRequired) {
					performer.enqueue(new ResetBeforeUpload(noSwap));
				}
				if (resetRequired) {
					if (eraseSettings)
						performer.enqueue(new EraseStorage());
					performer.enqueue(new Reset(noSwap));
				}

				performer.onTaskCompleted(Validate.this);
			}

			@Override
			public void onError(@NotNull final McuMgrException e) {
				performer.onTaskFailed(Validate.this, e);
			}
		});
	}

	private String parseMode(final int mode) {
		switch (mode) {
			case 0: return "Single App";
			case 1: return "Swap Scratch";
			case 2: return "Overwrite-only";
			case 3: return "Swap Without Scratch";
			case 4: return "Direct XIP Without Revert";
			case 5: return "Direct XIP With Revert";
			case 6: return "RAM Loader";
			default: return "Unknown (" + mode + ")";
		}
	}
}
