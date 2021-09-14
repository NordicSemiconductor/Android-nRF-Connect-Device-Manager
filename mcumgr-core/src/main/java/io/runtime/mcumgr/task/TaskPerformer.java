package io.runtime.mcumgr.task;

import org.jetbrains.annotations.NotNull;

import io.runtime.mcumgr.exception.McuMgrException;

public interface TaskPerformer<S> {
	void enqueue(@NotNull final Task<S> task);
	void onTaskCompleted();
	void onTaskFailed(@NotNull final McuMgrException error);
}
