package no.nordicsemi.android.mcumgr.task;

import org.jetbrains.annotations.NotNull;

import no.nordicsemi.android.mcumgr.McuMgrTransport;
import no.nordicsemi.android.mcumgr.exception.McuMgrException;

public interface TaskManager<S, State> {

	@NotNull
	McuMgrTransport getTransport();

	@NotNull
	S getSettings();

	void enqueue(@NotNull final Task<S, State> task);

	void onTaskProgressChanged(@NotNull final Task<S, State> task,
							   final int current, final int total, final long timestamp);

	void onTaskCompleted(@NotNull final Task<S, State> task);

	void onTaskFailed(@NotNull final Task<S, State> task, @NotNull final McuMgrException error);

}
