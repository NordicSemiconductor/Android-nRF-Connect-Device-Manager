package io.runtime.mcumgr.dfu.task;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.task.TaskPerformer;

class Reset extends FirmwareUpgradeTask {

	/**
	 * The timestamp at which the response to Reset command was received.
	 * Assuming that the target device has reset just after sending this response,
	 * the time difference between this moment and receiving disconnection event may be deducted
	 * from the estimated swap time.
	 */
	private long mResetResponseTime;

	Reset() {
	}

	@Override
	@NotNull
	public FirmwareUpgradeManager.State getState() {
		return FirmwareUpgradeManager.State.RESET;
	}

	@Override
	public int getPriority() {
		return PRIORITY_RESET;
	}

	@Override
	public void start(@NotNull final FirmwareUpgradeManager.Settings settings,
					  @NotNull final TaskPerformer<FirmwareUpgradeManager.Settings> performer) {
		//		mDefaultManager.getTransporter().addObserver(mResetObserver);
		//		mDefaultManager.reset(mResetCallback);
		Log.d("AAA", "Reset");
		performer.onTaskCompleted();
	}

//	/**
//	 * State: RESET.
//	 * Observer for the transport disconnection.
//	 */
//	private final McuMgrTransport.ConnectionObserver mResetObserver = new McuMgrTransport.ConnectionObserver() {
//		@Override
//		public void onConnected() {
//			// Do nothing
//		}
//
//		@Override
//		public void onDisconnected() {
//			mManager.getTransporter().removeObserver(mResetObserver);
//
//			LOG.info("Device disconnected");
//			Runnable reconnect = () -> mDefaultManager.getTransporter().connect(mReconnectCallback);
//			// Calculate the delay needed before verification.
//			// It may have taken 20 sec before the phone realized that it's
//			// disconnected. No need to wait more, perhaps?
//			long now = SystemClock.elapsedRealtime();
//			long timeSinceReset = now - mResetResponseTime;
//			long remainingTime = mEstimatedSwapTime - timeSinceReset;
//
//			if (remainingTime > 0) {
//				LOG.trace("Waiting for estimated swap time {}ms", mEstimatedSwapTime);
//				new Handler(Looper.getMainLooper()).postDelayed(reconnect, remainingTime);
//			} else {
//				reconnect.run();
//			}
//		}
//	};
//
//	/**
//	 * State: RESET.
//	 * Callback for the reset command.
//	 */
//	private final McuMgrCallback<McuMgrResponse> mResetCallback = new McuMgrCallback<McuMgrResponse>() {
//		@Override
//		public void onResponse(@NotNull McuMgrResponse response) {
//			// Check for an error return code
//			if (!response.isSuccess()) {
//				fail(new McuMgrErrorException(response.getReturnCode()));
//				return;
//			}
//			mResetResponseTime = SystemClock.elapsedRealtime();
//			LOG.trace("Reset request success. Waiting for disconnect...");
//		}
//
//		@Override
//		public void onError(@NotNull McuMgrException e) {
//			fail(e);
//		}
//	};
//
//	/**
//	 * State: RESET.
//	 * Callback for reconnecting to the device.
//	 */
//	private final McuMgrTransport.ConnectionCallback mReconnectCallback = new McuMgrTransport.ConnectionCallback() {
//
//		@Override
//		public void onConnected() {
//			LOG.info("Reconnect successful");
//			continueUpgrade();
//		}
//
//		@Override
//		public void onDeferred() {
//			LOG.info("Reconnect deferred");
//			continueUpgrade();
//		}
//
//		@Override
//		public void onError(@NotNull Throwable t) {
//			LOG.error("Reconnect failed");
//			fail(new McuMgrException(t));
//		}
//
//		public void continueUpgrade() {
//			switch (mState) {
//				case NONE:
//					// Upload cancelled in state validate.
//					cancelled(FirmwareUpgradeManager.State.VALIDATE);
//					break;
//				case VALIDATE:
//					// If the reset occurred in the validate state, we must re-validate as
//					// multiple resets may be required.
//					validate();
//					break;
//				case RESET:
//					switch (mMode) {
//						case TEST_AND_CONFIRM:
//							// The device reconnected after testing.
//							verify();
//							break;
//						case TEST_ONLY:
//						case CONFIRM_ONLY:
//							// The device has been tested or confirmed.
//							success();
//							break;
//					}
//			}
//		}
//	};
}
