package io.runtime.mcumgr.dfu.task;

import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.img.McuMgrImageStateResponse;
import io.runtime.mcumgr.task.TaskPerformer;

class Test extends FirmwareUpgradeTask {
	private final static Logger LOG = LoggerFactory.getLogger(Test.class);

	private final byte @NotNull [] hash;

	Test(final byte @NotNull [] hash) {
		this.hash = hash;
	}

	@Override
	@NotNull
	public FirmwareUpgradeManager.State getState() {
		return FirmwareUpgradeManager.State.TEST;
	}

	@Override
	public int getPriority() {
		return PRIORITY_TEST_AFTER_UPLOAD;
	}

	@Override
	public void start(@NotNull final FirmwareUpgradeManager.Settings settings,
					  @NotNull final TaskPerformer<FirmwareUpgradeManager.Settings> performer) {
		Log.d("AAA", "Test " + Arrays.toString(hash));
		performer.onTaskCompleted();
	}

//	/**
//	 * State: TEST.
//	 * Callback for the test command.
//	 */
//	private final McuMgrCallback<McuMgrImageStateResponse> mTestCallback = new McuMgrCallback<McuMgrImageStateResponse>() {
//		@Override
//		public void onResponse(@NotNull McuMgrImageStateResponse response) {
//			LOG.trace("Test response: {}", response.toString());
//			// Check for an error return code
//			if (!response.isSuccess()) {
//				fail(new McuMgrErrorException(response.getReturnCode()));
//				return;
//			}
//			if (response.images.length != 2) {
//				fail(new McuMgrException("Test response does not contain enough info"));
//				return;
//			}
//			if (!response.images[1].pending) {
//				fail(new McuMgrException("Tested image is not in a pending state."));
//				return;
//			}
//			// Test image success, begin device reset.
//			reset();
//		}
//
//		@Override
//		public void onError(@NotNull McuMgrException e) {
//			fail(e);
//		}
//	};
}
