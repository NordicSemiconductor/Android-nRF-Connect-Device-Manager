package io.runtime.mcumgr.task;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;
import io.runtime.mcumgr.exception.McuMgrException;

public abstract class TaskPerformer<S, State> {

	@Nullable
	private TaskManagerImpl manager;

	public TaskPerformer() {
	}

	public void start(@NotNull final S settings,
					  @NotNull final Task<S, State> task) {
		this.manager = new TaskManagerImpl(settings);

		onTaskStarted(null, task);
		task.start(manager);
	}

	@Nullable
	public Task<S, State> getCurrentTask() {
		if (manager != null) {
			return manager.currentTask;
		}
		return null;
	}

	public void pause() {
		if (manager != null) {
			manager.pause();
		}
	}

	public void resume() {
		if (manager != null) {
			manager.resume();
		}
	}

	public void cancel() {
		if (manager != null) {
			manager.cancel();
		}
	}

	public boolean isPaused() {
		if (manager != null) {
			return manager.isPaused();
		}
		return false;
	}

	public boolean isBusy() {
		return getCurrentTask() != null;
	}

	private class TaskManagerImpl implements TaskManager<S, State> {
		/**
		 * The queue of tasks to be performed during the update. The content of the queue
		 * depends on the images given in {@link FirmwareUpgradeManager#start(List, boolean)}
		 * and the state of the device, which is determined by validation step before the upload
		 * begins.
		 */
		@NotNull
		private final Queue<Task<S, State>> taskQueue = new PriorityQueue<>();

		/**
		 * The currently performed task.
		 */
		@Nullable
		private Task<S, State> currentTask;

		@NotNull
		private final S settings;

		private boolean paused;
		private boolean cancelled;

		private TaskManagerImpl(@NotNull final S settings) {
			this.settings = settings;
		}

		private void pause() {
			final Task<S, State> task = currentTask;
			if (paused || task == null)
				return;
			paused = true;
			task.pause();
		}

		private void resume() {
			final Task<S, State> task = currentTask;
			if (!paused || task == null)
				return;
			paused = false;
			task.start(this);
		}

		private void cancel() {
			final Task<S, State> task = currentTask;
			if (cancelled || task == null)
				return;
			cancelled = true;
			paused = false;
			task.cancel();
		}

		private boolean isPaused() {
			return paused;
		}

		@NotNull
		@Override
		public S getSettings() {
			return settings;
		}

		@Override
		public void enqueue(final @NotNull Task<S, State> task) {
			taskQueue.add(task);
		}

		@Override
		public void onTaskProgressChanged(final @NotNull Task<S, State> task,
										  final int current, final int total, final long timestamp) {
			TaskPerformer.this.onTaskProgressChanged(task, current, total, timestamp);
		}

		@Override
		public void onTaskCompleted(final @NotNull Task<S, State> task) {
			// Has the process been cancelled?
			if (cancelled) {
				cleanUp();
				TaskPerformer.this.onCancelled(task);
				return;
			}

			// Poll the next task. If there's nothing, we're done.
			final Task<S, State> nextTask = currentTask = taskQueue.poll();
			if (nextTask == null) {
				cleanUp();
				TaskPerformer.this.onCompleted(task);
				return;
			}

			TaskPerformer.this.onTaskStarted(task, nextTask);

			// Should we pause a bit?
			if (paused) {
				return;
			}

			// Run the next task.
			nextTask.start(this);
		}

		@Override
		public void onTaskFailed(final @NotNull Task<S, State> task,
								 final @NotNull McuMgrException error) {
			cleanUp();
			TaskPerformer.this.onTaskFailed(task, error);
		}

		private void cleanUp() {
			taskQueue.clear();
			currentTask = null;
			paused = false;
			cancelled = false;
			TaskPerformer.this.cleanUp();
		}
	}

	public void onTaskProgressChanged(final @NotNull Task<S, State> task,
									  final int current, final int total, final long timestamp) {

	}

	public void onTaskStarted(@Nullable final Task<S, State> previousTask,
							  @NotNull final Task<S, State> newTask) {

	}

	public void onCancelled(@NotNull final Task<S, State> task) {

	}

	public void onCompleted(@NotNull final Task<S, State> task) {

	}

	public void onTaskFailed(@NotNull final Task<S, State> task,
							 @NotNull final McuMgrException error) {
	}

	private void cleanUp() {
		manager = null;
	}
}
