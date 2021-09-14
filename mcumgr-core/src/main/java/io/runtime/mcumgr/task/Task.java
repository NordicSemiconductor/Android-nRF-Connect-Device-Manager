package io.runtime.mcumgr.task;

import org.jetbrains.annotations.NotNull;

import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;
import io.runtime.mcumgr.exception.McuMgrException;

public abstract class Task<S> implements Comparable<Task<S>> {

	protected Task() {
	}

	/**
	 * Returns task priority. Tasks are added to a priority queue and must be executed in
	 * the right order, but can be added to the list in any order.
	 * @return The task priority.
	 */
	public abstract int getPriority();

	public abstract void start(@NotNull final S settings,
							   @NotNull final TaskPerformer<S> performer);

	public void pause() {
		// Empty default implementation.
	}

	public void cancel() {
		// Empty default implementation.
	}

	@Override
	public final int compareTo(final Task<S> o) {
		return getPriority() - o.getPriority();
	}
}
