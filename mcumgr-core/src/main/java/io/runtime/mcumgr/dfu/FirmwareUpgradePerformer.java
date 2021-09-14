package io.runtime.mcumgr.dfu;

import android.util.Pair;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import io.runtime.mcumgr.dfu.task.FirmwareUpgradeTask;
import io.runtime.mcumgr.dfu.task.PerformDfu;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.image.McuMgrImage;
import io.runtime.mcumgr.task.Task;
import io.runtime.mcumgr.task.TaskPerformer;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager.Mode;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager.State;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager.Settings;

class FirmwareUpgradePerformer implements TaskPerformer<Settings> {
	private final static Logger LOG = LoggerFactory.getLogger(FirmwareUpgradePerformer.class);

	/**
	 * Firmware upgrade callback passed into the constructor or set before the upload has started.
	 */
	@NotNull
	private final FirmwareUpgradeCallback callback;

	/**
	 * The queue of tasks to be performed during the update. The content of the queue
	 * depends on the images given in {@link FirmwareUpgradeManager#start(List)} and the state of the device,
	 * which is determined by validation step before the upload begins.
	 */
	@NotNull
	private final Queue<FirmwareUpgradeTask> taskQueue = new PriorityQueue<>();

	/**
	 * The currently performed task.
	 */
	@Nullable
	private FirmwareUpgradeTask currentTask;

	@Nullable
	private Settings settings;

	private boolean paused;
	private boolean cancelled;

	FirmwareUpgradePerformer(@NotNull final FirmwareUpgradeCallback callback) {
		this.callback = callback;
	}

	State getState() {
		final FirmwareUpgradeTask task = currentTask;
		if (task == null)
			return State.NONE;
		return task.getState();
	}

	void start(@NotNull final Mode mode,
			   @NotNull final List<Pair<Integer, McuMgrImage>> images,
			   @NotNull final Settings settings) {
		this.settings = settings;
		enqueue(new PerformDfu(mode, images));
		onTaskCompleted();
	}

	synchronized void pause() {
		final FirmwareUpgradeTask task = currentTask;
		if (paused || task == null)
			return;
		paused = true;
		task.pause();
	}

	synchronized void resume() {
		final Task<Settings> task = currentTask;
		if (!paused || task == null || settings == null)
			return;
		paused = false;
		task.start(settings, this);
	}

	synchronized void cancel() {
		final Task<Settings> task = currentTask;
		if (!cancelled || task == null)
			return;
		cancelled = true;
		paused = false;
		task.cancel();
	}

	synchronized boolean isPaused() {
		return paused;
	}

	boolean isBusy() {
		return currentTask != null;
	}

	@Override
	public void enqueue(@NotNull final Task<Settings> task) {
		taskQueue.add((FirmwareUpgradeTask) task);
	}

	@Override
	public void onTaskCompleted() {
		final FirmwareUpgradeTask completedTask = currentTask;
		final FirmwareUpgradeManager.State prevState = completedTask != null ?
				completedTask.getState() : FirmwareUpgradeManager.State.NONE;

		// Has the process been cancelled?
		if (cancelled) {
			cleanUp();
			callback.onUpgradeCanceled(prevState);
			return;
		}

		// Poll the next task. If there's nothing, we're done.
		final FirmwareUpgradeTask nextTask = currentTask = taskQueue.poll();
		if (nextTask == null) {
			callback.onUpgradeCompleted();
			return;
		}

		// Notify observer about changing the state.
		final FirmwareUpgradeManager.State newState = nextTask.getState();
		if (newState != prevState) {
			LOG.trace("Moving from state {} to state {}", prevState.name(), newState.name());
			callback.onStateChanged(prevState, newState);
		}

		// Should we pause a bit?
		if (paused) {
			return;
		}

		// Run the next task.
		assert settings != null;
		nextTask.start(settings,this);
	}

	@Override
	public void onTaskFailed(@NotNull final McuMgrException error) {
		final FirmwareUpgradeTask task = currentTask;
		if (task == null)
			return;
		cleanUp();
		callback.onUpgradeFailed(task.getState(), error);
	}

	private void cleanUp() {
		taskQueue.clear();
		currentTask = null;
		paused = false;
		cancelled = false;
		settings = null;
	}

}
