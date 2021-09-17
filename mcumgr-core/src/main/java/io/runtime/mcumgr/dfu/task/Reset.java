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
				transport.removeObserver(this);

				LOG.info("Device disconnected");
				final Runnable reconnect = () -> transport.connect(new McuMgrTransport.ConnectionCallback() {
					@Override
					public void onConnected() {
						performer.onTaskCompleted(Reset.this);
					}

					@Override
					public void onDeferred() {
						performer.onTaskCompleted(Reset.this);
					}

					@Override
					public void onError(@NotNull final Throwable t) {
						performer.onTaskFailed(Reset.this, new McuMgrException(t));
					}
				});
				// Calculate the delay needed before verification.
				// It may have taken 20 sec before the phone realized that it's
				// disconnected. No need to wait more, perhaps?
				long now = SystemClock.elapsedRealtime();
				long timeSinceReset = now - mResetResponseTime;
				long remainingTime = settings.estimatedSwapTime - timeSinceReset;

				if (remainingTime > 0) {
					LOG.trace("Waiting for estimated swap time {} ms", settings.estimatedSwapTime);
					new Handler().postDelayed(reconnect, remainingTime);
				} else {
					reconnect.run();
				}
			}
		});

		final DefaultManager manager = new DefaultManager(transport);
		manager.reset(new McuMgrCallback<McuMgrResponse>() {
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
