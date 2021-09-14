package io.runtime.mcumgr.dfu.task;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;
import io.runtime.mcumgr.task.TaskPerformer;

class Confirm extends FirmwareUpgradeTask {
	private final byte @NotNull [] hash;

	Confirm(final byte @NotNull [] hash) {
		this.hash = hash;
	}

	@Override
	@NotNull
	public FirmwareUpgradeManager.State getState() {
		return FirmwareUpgradeManager.State.CONFIRM;
	}

	@Override
	public int getPriority() {
		return PRIORITY_CONFIRM_AFTER_UPLOAD;
	}

	@Override
	public void start(@NotNull final FirmwareUpgradeManager.Settings settings,
					  @NotNull final TaskPerformer<FirmwareUpgradeManager.Settings> performer) {
		Log.d("AAA", "Confirm " + Arrays.toString(hash));
		performer.onTaskCompleted();
	}

//	/**
//	 * State: CONFIRM.
//	 * Callback for the confirm command.
//	 */
//	private final McuMgrCallback<McuMgrImageStateResponse> mConfirmCallback = new McuMgrCallback<McuMgrImageStateResponse>() {
//		private final static int MAX_ATTEMPTS = 2;
//		private int mAttempts = 0;
//
//		@Override
//		public void onResponse(@NotNull McuMgrImageStateResponse response) {
//			// Reset retry counter
//			mAttempts = 0;
//
//			LOG.trace("Confirm response: {}", response.toString());
//			// Check for an error return code
//			if (!response.isSuccess()) {
//				fail(new McuMgrErrorException(response.getReturnCode()));
//				return;
//			}
//			if (response.images.length == 0) {
//				fail(new McuMgrException("Confirm response does not contain enough info"));
//				return;
//			}
//			// Handle the response based on mode.
//			switch (mMode) {
//				case CONFIRM_ONLY:
//					// Check that an image exists in slot 1
//					if (response.images.length != 2) {
//						fail(new McuMgrException("Confirm response does not contain enough info"));
//						return;
//					}
//					// Check that the upgrade image has been confirmed
//					if (!response.images[1].pending) {
//						fail(new McuMgrException("Image is not in a confirmed state."));
//						return;
//					}
//					// Reset the device, we don't want to do anything more.
//					reset();
//					break;
//				case TEST_AND_CONFIRM:
//					// Check that the upgrade image has successfully booted
//					if (!Arrays.equals(mHash, response.images[0].hash)) {
//						fail(new McuMgrException("Device failed to boot into new image"));
//						return;
//					}
//					// Check that the upgrade image has been confirmed
//					if (!response.images[0].confirmed) {
//						fail(new McuMgrException("Image is not in a confirmed state."));
//						return;
//					}
//					// The device has been tested and confirmed.
//					success();
//					break;
//			}
//		}
//
//		@Override
//		public void onError(@NotNull McuMgrException e) {
//			// The confirm request might have been sent after the device was rebooted
//			// and the images were swapped. Swapping images, depending on the hardware,
//			// make take a long time, during which the phone may throw 133 error as a
//			// timeout. In such case we should try again.
//			if (e instanceof McuMgrTimeoutException) {
//				if (mAttempts++ < MAX_ATTEMPTS) {
//					// Try again
//					LOG.warn("Connection timeout. Retrying...");
//					verify();
//					return;
//				}
//			}
//			fail(e);
//		}
//	};
}
