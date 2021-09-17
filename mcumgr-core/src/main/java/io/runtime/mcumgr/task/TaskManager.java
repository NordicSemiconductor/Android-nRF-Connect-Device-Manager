package io.runtime.mcumgr.task;

import org.jetbrains.annotations.NotNull;

import io.runtime.mcumgr.exception.McuMgrException;

public interface TaskManager<S, State> {

	@NotNull
	S getSettings();

	void enqueue(@NotNull final Task<S, State> task);

	void onTaskProgressChanged(@NotNull final Task<S, State> task,
							   final int current, final int total, final long timestamp);

	void onTaskCompleted(@NotNull final Task<S, State> task);

	void onTaskFailed(@NotNull final Task<S, State> task, @NotNull final McuMgrException error);

}
