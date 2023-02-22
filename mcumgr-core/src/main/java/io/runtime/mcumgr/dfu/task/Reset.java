package io.runtime.mcumgr.dfu.task;

import android.os.Handler;
import android.os.SystemClock;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager.Settings;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager.State;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.managers.DefaultManager;
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.task.TaskManager;

class Reset extends FirmwareUpgradeTask {
	private final static Logger LOG = LoggerFactory.getLogger(Reset.class);

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
	public State getState() {
		return State.RESET;
	}

	@Override
	public int getPriority() {
		return PRIORITY_RESET;
	}

	@Override
	public void start(@NotNull final TaskManager<Settings, State> performer) {
		final Settings settings = performer.getSettings();
		final McuMgrTransport transport = settings.transport;

		transport.addObserver(new McuMgrTransport.ConnectionObserver() {
			@Override
			public void onConnected() {
				// Do nothing
			}

			@Override
			public void onDisconnected() {
				LOG.info("Device disconnected");

				transport.removeObserver(this);

				// Calculate the delay need that we need to wait until the swap is complete.
				long now = SystemClock.elapsedRealtime();
				long timeSinceReset = now - mResetResponseTime;
				long remainingTime = settings.estimatedSwapTime - timeSinceReset;
				final Runnable complete = () -> performer.onTaskCompleted(Reset.this);

				if (remainingTime > 0) {
					LOG.trace("Waiting remaining {} ms for the swap operation to complete", remainingTime);
					new Handler().postDelayed(complete, remainingTime);
				} else {
					complete.run();
				}
			}
		});

		final DefaultManager manager = new DefaultManager(transport);
		manager.reset(new McuMgrCallback<>() {
			@Override
			public void onResponse(@NotNull final McuMgrResponse response) {
				// Check for an error return code.
				if (!response.isSuccess()) {
					performer.onTaskFailed(Reset.this, new McuMgrErrorException(response.getReturnCode()));
					return;
				}
				mResetResponseTime = SystemClock.elapsedRealtime();
				LOG.trace("Reset request success. Waiting for disconnect...");
			}

			@Override
			public void onError(@NotNull final McuMgrException error) {
				performer.onTaskFailed(Reset.this, error);
			}
		});
	}
}
