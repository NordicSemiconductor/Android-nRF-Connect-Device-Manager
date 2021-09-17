package io.runtime.mcumgr.dfu;

import android.util.Pair;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import io.runtime.mcumgr.dfu.FirmwareUpgradeManager.Mode;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager.Settings;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager.State;
import io.runtime.mcumgr.dfu.task.FirmwareUpgradeTask;
import io.runtime.mcumgr.dfu.task.PerformDfu;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.image.McuMgrImage;
import io.runtime.mcumgr.task.Task;
import io.runtime.mcumgr.task.TaskPerformer;

public class FirmwareUpgradePerformer extends TaskPerformer<Settings, State> {
	private final static Logger LOG = LoggerFactory.getLogger(FirmwareUpgradePerformer.class);

	/**
	 * Firmware upgrade callback passed into the constructor or set before the upload has started.
	 */
	@NotNull
	private final FirmwareUpgradeCallback callback;

	FirmwareUpgradePerformer(@NotNull final FirmwareUpgradeCallback callback) {
		this.callback = callback;
	}

	State getState() {
		final FirmwareUpgradeTask task = (FirmwareUpgradeTask) getCurrentTask();
		if (task == null)
			return State.NONE;
		return task.getState();
	}

	void start(@NotNull final Settings settings,
			   @NotNull final Mode mode,
			   @NotNull final List<Pair<Integer, McuMgrImage>> images,
			   final boolean eraseSettings) {
		LOG.trace("Starting DFU, mode: {}", mode.name());
		super.start(settings, new PerformDfu(mode, images, eraseSettings));
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
