package io.runtime.mcumgr.dfu.mcuboot.task;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.dfu.mcuboot.FirmwareUpgradeManager.Settings;
import io.runtime.mcumgr.dfu.mcuboot.FirmwareUpgradeManager.State;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.managers.BasicManager;
import io.runtime.mcumgr.response.zephyr.basic.McuMgrZephyrBasicResponse;
import io.runtime.mcumgr.task.TaskManager;

class EraseStorage extends FirmwareUpgradeTask {
	private final static Logger LOG = LoggerFactory.getLogger(EraseStorage.class);

	EraseStorage() {
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
		final BasicManager manager = new BasicManager(performer.getTransport());
		manager.eraseStorage(new McuMgrCallback<>() {
			@Override
			public void onResponse(@NotNull final McuMgrZephyrBasicResponse response) {
				LOG.trace("Erase storage response: {}", response);

				// Fix: If this feature is not supported on the device, this should not cause fail
				//      the process.

				// Check for an error return code.
				// if (!response.isSuccess()) {
				//    performer.onTaskFailed(EraseSettings.this, new McuMgrErrorException(response.getReturnCode()));
				//	  return;
				// }
				performer.onTaskCompleted(EraseStorage.this);
			}

			@Override
			public void onError(@NotNull McuMgrException e) {
				// If Erase is not supported, proceed with the update.
				// Erase Storage has been added in NCS 1.8.
				if (e instanceof McuMgrErrorException) {
					final McuMgrErrorException error = (McuMgrErrorException) e;
					if (error.getCode() == McuMgrErrorCode.NOT_SUPPORTED || error.getGroupCode() != null) {
						performer.onTaskCompleted(EraseStorage.this);
						return;
					}
				}
				performer.onTaskFailed(EraseStorage.this, e);
			}
		});
	}
}
