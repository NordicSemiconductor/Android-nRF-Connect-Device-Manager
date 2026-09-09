package no.nordicsemi.android.mcumgr.task;

import org.jetbrains.annotations.NotNull;

import no.nordicsemi.android.mcumgr.McuMgrTransport;
import no.nordicsemi.android.mcumgr.exception.McuMgrException;
import no.nordicsemi.android.mcumgr.log.McuMgrLogger;

public interface TaskManager<S, State> {

	/**
	 * Returns the logger of the manager owning the performer that runs the task. Tasks should
	 * log through it, so that all entries of a single firmware upgrade share a category
	 * and a sink.
	 */
	@NotNull
	McuMgrLogger getLog();

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
