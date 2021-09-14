package io.runtime.mcumgr.dfu.task;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;
import io.runtime.mcumgr.image.McuMgrImage;
import io.runtime.mcumgr.task.TaskPerformer;
import io.runtime.mcumgr.transfer.TransferController;

class Upload extends FirmwareUpgradeTask {
	private final McuMgrImage mcuMgrImage;
	private final int image;

	/**
	 * Upload controller used to pause, resume, and cancel upload. Set when the upload is started.
	 */
	private TransferController mUploadController;

	Upload(@NotNull final McuMgrImage mcuMgrImage, final int image) {
		this.mcuMgrImage = mcuMgrImage;
		this.image = image;
	}

	@Override
	@NotNull
	public FirmwareUpgradeManager.State getState() {
		return FirmwareUpgradeManager.State.UPLOAD;
	}

	@Override
	public int getPriority() {
		return PRIORITY_UPLOAD;
	}

	@Override
	public void start(@NotNull final FirmwareUpgradeManager.Settings settings,
					  @NotNull final TaskPerformer<FirmwareUpgradeManager.Settings> performer) {
//		if (mWindowCapacity > 1) {
//			mUploadController = windowUpload(mImageManager, mImageData, mWindowCapacity,
//					mImageUploadCallback);
//		} else {
//			mUploadController = mImageManager.imageUpload(mImageData, mImageUploadCallback);
//		}
		Log.d("AAA", "Uploading " + image);
		performer.onTaskCompleted();
	}

	@Override
	public void pause() {

	}

	@Override
	public void cancel() {

	}

	//******************************************************************
	// Image Upload Callback
	//******************************************************************

//	/**
//	 * Image upload callback. Forwards upload callbacks to the FirmwareUpgradeCallback.
//	 */
//	private final UploadCallback mImageUploadCallback = new UploadCallback() {
//
//		@Override
//		public void onUploadProgressChanged(int current, int total, long timestamp) {
//			mInternalCallback.onUploadProgressChanged(current, total, timestamp);
//		}
//
//		@Override
//		public void onUploadFailed(@NotNull McuMgrException error) {
//			fail(error);
//		}
//
//		@Override
//		public void onUploadCanceled() {
//			cancelled(FirmwareUpgradeManager.State.UPLOAD);
//		}
//
//		@Override
//		public void onUploadCompleted() {
//			// When upload is complete, send test on confirm commands, depending on the mode.
//			switch (mMode) {
//				case TEST_ONLY:
//				case TEST_AND_CONFIRM:
//					test();
//					break;
//				case CONFIRM_ONLY:
//					confirm();
//					break;
//			}
//		}
//	};
}
