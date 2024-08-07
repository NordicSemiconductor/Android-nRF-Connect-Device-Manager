package io.runtime.mcumgr.task;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.PriorityQueue;
import java.util.Queue;

import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.McuMgrException;

public abstract class TaskPerformer<S, State> {

	@Nullable
	private TaskManagerImpl manager;

	public TaskPerformer() {
	}

	public void start(@NotNull final McuMgrTransport transport,
					  @NotNull final S settings,
					  @NotNull final Task<S, State> task) {
		this.manager = new TaskManagerImpl(transport, settings);

		try {
			onTaskStarted(null, task);
		} catch (Exception ignored) {}
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
		@NotNull
		private final McuMgrTransport transport;

		/**
		 * The queue of tasks to be performed.
		 * <p>
		 * The tasks may be added to the queue by calling {@link #enqueue(Task)}.
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

		private TaskManagerImpl(@NotNull final McuMgrTransport transport,
								@NotNull final S settings) {
			this.transport = transport;
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

		@NotNull
		@Override
		public McuMgrTransport getTransport() {
			return transport;
		}

		@Override
		public void enqueue(final @NotNull Task<S, State> task) {
			taskQueue.add(task);
		}

		@Override
		public void onTaskProgressChanged(final @NotNull Task<S, State> task,
										  final int current, final int total, final long timestamp) {
			try {
				TaskPerformer.this.onTaskProgressChanged(task, current, total, timestamp);
			} catch (Exception ignored) {}
		}

		@Override
		public void onTaskCompleted(final @NotNull Task<S, State> task) {
			// Has the process been cancelled?
			if (cancelled) {
				cleanUp();
				try {
					TaskPerformer.this.onCancelled(task);
				} catch (Exception ignored) {}
				return;
			}

			// Poll the next task. If there's nothing, we're done.
			final Task<S, State> nextTask = currentTask = taskQueue.poll();
			if (nextTask == null) {
				cleanUp();
				try {
					TaskPerformer.this.onCompleted(task);
				} catch (Exception ignored) {}
				return;
			}

			try {
				TaskPerformer.this.onTaskStarted(task, nextTask);
			} catch (Exception ignored) {}

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
			try {
				TaskPerformer.this.onTaskFailed(task, error);
			} catch (Exception ignored) {}
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
