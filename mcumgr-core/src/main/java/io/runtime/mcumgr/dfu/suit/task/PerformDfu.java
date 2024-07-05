package io.runtime.mcumgr.dfu.suit.task;

import org.jetbrains.annotations.NotNull;

import io.runtime.mcumgr.dfu.suit.SUITUpgradeManager;
import io.runtime.mcumgr.dfu.suit.SUITUpgradePerformer;
import io.runtime.mcumgr.task.TaskManager;

/**
 * This task performs the DFU using SUIT manager.
 */
public class PerformDfu extends SUITUpgradeTask {

	private final byte @NotNull [] envelope;

	public PerformDfu(final byte @NotNull [] envelope) {
		this.envelope = envelope;
	}

	@NotNull
	@Override
	public SUITUpgradeManager.State getState() {
		return SUITUpgradeManager.State.PROCESSING;
	}

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public void start(final @NotNull TaskManager<SUITUpgradePerformer.Settings, SUITUpgradeManager.State> performer) {
		performer.enqueue(new UploadEnvelope(envelope));
		performer.onTaskCompleted(this);
	}
}
