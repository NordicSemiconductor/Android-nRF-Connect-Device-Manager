package io.runtime.mcumgr.dfu.task;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager.Settings;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager.State;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.managers.BasicManager;
import io.runtime.mcumgr.managers.ImageManager;
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.response.img.McuMgrImageStateResponse;
import io.runtime.mcumgr.task.TaskManager;

class EraseSettings extends FirmwareUpgradeTask {
	private final static Logger LOG = LoggerFactory.getLogger(EraseSettings.class);

	EraseSettings() {
	}

	@Override
	@NotNull
	public State getState() {
		return State.UPLOAD;
	}

	@Override
	public int getPriority() {
		return PRIORITY_ERASE_SETTINGS;
	}

	@Override
	public void start(final @NotNull TaskManager<Settings, State> performer) {
		final Settings settings = performer.getSettings();
		final BasicManager manager = new BasicManager(settings.transport);
		manager.eraseStorage(new McuMgrCallback<McuMgrResponse>() {
			@Override
			public void onResponse(@NotNull final McuMgrResponse response) {
				LOG.trace("Erase settings response: {}", response.toString());

				// Fix: If this feature is not supported on the device, this should not cause fail the process.
				// Check for an error return code.
				// if (!response.isSuccess()) {
				//    performer.onTaskFailed(EraseSettings.this, new McuMgrErrorException(response.getReturnCode()));
				//	  return;
				// }
				performer.onTaskCompleted(EraseSettings.this);
			}

			@Override
			public void onError(@NotNull McuMgrException e) {
				performer.onTaskFailed(EraseSettings.this, e);
			}
		});
	}
}
