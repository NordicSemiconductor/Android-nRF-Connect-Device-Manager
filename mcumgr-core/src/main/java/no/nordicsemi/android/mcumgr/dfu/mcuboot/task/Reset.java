package no.nordicsemi.android.mcumgr.dfu.mcuboot.task;

import android.os.Handler;
import android.os.SystemClock;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

import no.nordicsemi.android.mcumgr.McuMgrCallback;
import no.nordicsemi.android.mcumgr.McuMgrTransport;
import no.nordicsemi.android.mcumgr.dfu.mcuboot.FirmwareUpgradeManager.Settings;
import no.nordicsemi.android.mcumgr.dfu.mcuboot.FirmwareUpgradeManager.State;
import no.nordicsemi.android.mcumgr.exception.McuMgrErrorException;
import no.nordicsemi.android.mcumgr.exception.McuMgrException;
import no.nordicsemi.android.mcumgr.managers.DefaultManager;
import no.nordicsemi.android.mcumgr.managers.SettingsManager;
import no.nordicsemi.android.mcumgr.response.McuMgrResponse;
import no.nordicsemi.android.mcumgr.response.dflt.McuMgrOsResponse;
import no.nordicsemi.android.mcumgr.task.TaskManager;

class Reset extends FirmwareUpgradeTask {
	private final static Logger LOG = LoggerFactory.getLogger(Reset.class);

	/**
	 * The key used to set the advertising name for the Firmware Loader mode.
	 * <p>
	 * This uses the Settings manager.
	 */
	private final static String KEY_SET_NAME = "fw_loader/adv_name";

	/**
	 * The timestamp at which the response to Reset command was received.
	 * Assuming that the target device has reset just after sending this response,
	 * the time difference between this moment and receiving disconnection event may be deducted
	 * from the estimated swap time.
	 */
	private long mResetResponseTime;

	private final boolean mNoSwap;
	private final int mBootMode;
	private final String mAdvName;

	Reset(final boolean noSwap) {
		this.mNoSwap = noSwap;
		this.mBootMode = 0;
		this.mAdvName = null;
	}

	Reset(final @NotNull String advName) {
		this.mNoSwap = true;
		this.mBootMode = DefaultManager.BOOT_MODE_TYPE_BOOTLOADER;
		this.mAdvName = advName;
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
		final McuMgrTransport transport = performer.getTransport();

		transport.addObserver(new McuMgrTransport.ConnectionObserver() {
			@Override
			public void onConnected() {
				// Do nothing
			}

			@Override
			public void onDisconnected() {
				LOG.info("Device disconnected");

				transport.removeObserver(this);

				// If there is no swap, we're done. No need to wait anything.
				if (mNoSwap) {
					performer.onTaskCompleted(Reset.this);
					return;
				}

				// Calculate the delay need that we need to wait until the swap is complete.
				long now = SystemClock.elapsedRealtime();
				if (mResetResponseTime == 0) {
					// In case the response to Reset command wasn't received before the disconnection
					// start counting remaining time from now.
					mResetResponseTime = now;
				}
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

		if (mAdvName != null) {
			setName(mAdvName, performer, () -> reset(mBootMode, performer));
			return;
		}
		reset(mBootMode, performer);
	}

	private void setName(@NotNull final String advName, @NotNull final TaskManager<Settings, State> performer, @NotNull final Runnable then) {
		final McuMgrTransport transport = performer.getTransport();
		final SettingsManager manager = new SettingsManager(transport);

		LOG.trace("Switching to firmware loader (name: {})...", mAdvName);

		// Setting the name uses Settings group. The name is sent as bytes, not a string.
		final byte[] nameData = advName.getBytes(StandardCharsets.UTF_8);
		manager.write(KEY_SET_NAME, nameData, new McuMgrCallback<>() {
			@Override
			public void onResponse(@NotNull final McuMgrResponse response) {
				LOG.trace("Saving settings...");
				manager.save(new McuMgrCallback<>() {
                    @Override
                    public void onResponse(@NotNull McuMgrResponse response) {
						LOG.info("Firmware Loader name set to: {}", advName);
                        then.run();
                    }

                    @Override
                    public void onError(@NotNull McuMgrException error) {
						LOG.error("Failed to save settings", error);
                        performer.onTaskFailed(Reset.this, error);
                    }
                });
			}

			@Override
			public void onError(@NotNull final McuMgrException error) {
				LOG.error("Failed to set Firmware Loader advertising name. Connect to the device in this mode manually", error);
				performer.onTaskFailed(Reset.this, new McuMgrException("Connect to the Firmware Loader manually and retry"));
			}
		});
	}

	private void reset(final int bootMode, @NotNull final TaskManager<Settings, State> performer) {
		final McuMgrTransport transport = performer.getTransport();
		final DefaultManager manager = new DefaultManager(transport);

		LOG.trace("Resetting (boot mode: {})...", bootMode);
		manager.reset(bootMode, false, new McuMgrCallback<>() {
			@Override
			public void onResponse(@NotNull final McuMgrOsResponse response) {
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
				// A McuMgrDisconnectedException may be returned if the device has disconnected
				// before the response was received. In this case, assume that the reset
				// was successful and the device is now disconnected.
				// See: https://github.com/NordicSemiconductor/Android-nRF-Connect-Device-Manager/issues/242
			}
		});
	}
}
