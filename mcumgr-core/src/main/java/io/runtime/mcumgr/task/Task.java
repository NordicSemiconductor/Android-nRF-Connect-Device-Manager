package io.runtime.mcumgr.task;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Task<S, State> implements Comparable<Task<S, State>> {

	protected Task() {
	}

	/**
	 * Returns task priority. Tasks are added to a priority queue and must be executed in
	 * the right order, but can be added to the list in any order.
	 * @return The task priority.
	 */
	public abstract int getPriority();
	/**
	 * Returns the state that will be reported to the callback.
	 * @return The state in which the manager is in.
	 */
	@Nullable
	public abstract State getState();

	public abstract void start(@NotNull final TaskManager<S, State> performer);

	public void pause() {
		// Empty default implementation.
	}

	public void cancel() {
		// Empty default implementation.
	}

	@Override
	public final int compareTo(final Task<S, State> o) {
		return getPriority() - o.getPriority();
	}
}
