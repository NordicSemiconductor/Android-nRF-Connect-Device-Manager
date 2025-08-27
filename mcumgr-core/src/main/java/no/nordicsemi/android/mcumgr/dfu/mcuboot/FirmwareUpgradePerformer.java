package no.nordicsemi.android.mcumgr.dfu.mcuboot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nordicsemi.android.mcumgr.McuMgrTransport;
import no.nordicsemi.android.mcumgr.dfu.FirmwareUpgradeCallback;
import no.nordicsemi.android.mcumgr.dfu.mcuboot.FirmwareUpgradeManager.Mode;
import no.nordicsemi.android.mcumgr.dfu.mcuboot.FirmwareUpgradeManager.Settings;
import no.nordicsemi.android.mcumgr.dfu.mcuboot.FirmwareUpgradeManager.State;
import no.nordicsemi.android.mcumgr.dfu.mcuboot.model.ImageSet;
import no.nordicsemi.android.mcumgr.dfu.mcuboot.task.FirmwareUpgradeTask;
import no.nordicsemi.android.mcumgr.dfu.mcuboot.task.PerformDfu;
import no.nordicsemi.android.mcumgr.exception.McuMgrException;
import no.nordicsemi.android.mcumgr.task.Task;
import no.nordicsemi.android.mcumgr.task.TaskPerformer;

public class FirmwareUpgradePerformer extends TaskPerformer<Settings, State> {
	private final static Logger LOG = LoggerFactory.getLogger(FirmwareUpgradePerformer.class);

	/**
	 * Firmware upgrade callback passed into the constructor or set before the upload has started.
	 */
	@NotNull
	private final FirmwareUpgradeCallback<State> callback;

	FirmwareUpgradePerformer(@NotNull final FirmwareUpgradeCallback<State> callback) {
		this.callback = callback;
	}

	State getState() {
		final FirmwareUpgradeTask task = (FirmwareUpgradeTask) getCurrentTask();
		if (task == null)
			return State.NONE;
		return task.getState();
	}

	void start(@NotNull final McuMgrTransport transport,
			   @NotNull final Settings settings,
			   @NotNull final ImageSet images,
			   @NotNull final Mode mode) {
		LOG.trace("Starting DFU, mode: {}", mode.name());
		super.start(transport, settings, new PerformDfu(mode, images));
	}

	@Override
	public void onTaskStarted(final @Nullable Task<Settings, State> previousTask,
							  final @NotNull Task<Settings, State> nextTask) {
		if (previousTask == null)
			return;

		// Notify observer about changing the state.
		final State oldState = previousTask.getState();
		final State newState = nextTask.getState();
		if (oldState != null && newState != null && newState != oldState) {
			LOG.trace("Moving from state {} to state {}", oldState.name(), newState.name());
			callback.onStateChanged(oldState, newState);
		}
	}

	@Override
	public void onCompleted(final @NotNull Task<Settings, State> task) {
		callback.onUpgradeCompleted();
	}

	@Override
	public void onTaskProgressChanged(final @NotNull Task<Settings, State> task,
									  final int current, final int total, final long timestamp) {
		callback.onUploadProgressChanged(current, total, timestamp);
	}

	@Override
	public void onCancelled(final @NotNull Task<Settings, State> task) {
		callback.onUpgradeCanceled(task.getState());
	}

	@Override
	public void onTaskFailed(final @NotNull Task<Settings, State> task,
							 final @NotNull McuMgrException error) {
		callback.onUpgradeFailed(task.getState(), error);
	}
}
