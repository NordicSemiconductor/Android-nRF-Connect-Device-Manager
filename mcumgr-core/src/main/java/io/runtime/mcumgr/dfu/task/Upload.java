package io.runtime.mcumgr.dfu.task;

import org.jetbrains.annotations.NotNull;

import io.runtime.mcumgr.dfu.FirmwareUpgradeManager.Settings;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager.State;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.image.McuMgrImage;
import io.runtime.mcumgr.managers.ImageManager;
import io.runtime.mcumgr.task.TaskManager;
import io.runtime.mcumgr.transfer.TransferController;
import io.runtime.mcumgr.transfer.UploadCallback;

import static io.runtime.mcumgr.transfer.ImageUploaderKt.windowUpload;

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
	public State getState() {
		return State.UPLOAD;
	}

	@Override
	public int getPriority() {
		return PRIORITY_UPLOAD;
	}

	@Override
	public void start(@NotNull final TaskManager<Settings, State> performer) {
		// Should we resume?
		if (mUploadController != null) {
			mUploadController.resume();
			return;
		}

		final UploadCallback callback = new UploadCallback() {
			@Override
			public void onUploadProgressChanged(final int current, final int total, final long timestamp) {
				performer.onTaskProgressChanged(Upload.this, current, total, timestamp);
			}

			@Override
			public void onUploadFailed(@NotNull final McuMgrException error) {
				performer.onTaskFailed(Upload.this, error);
			}

			@Override
			public void onUploadCanceled() {
				performer.onTaskCompleted(Upload.this);
			}

			@Override
			public void onUploadCompleted() {
				performer.onTaskCompleted(Upload.this);
			}
		};

		final Settings settings = performer.getSettings();
		final ImageManager manager = new ImageManager(settings.transport);
		if (settings.windowCapacity > 1) {
			mUploadController = windowUpload(
					manager,
					mcuMgrImage.getData(), image,
					settings.windowCapacity,
					settings.memoryAlignment,
					callback
			);
		} else {
			mUploadController = manager.imageUpload(mcuMgrImage.getData(), image, callback);
		}
	}

	@Override
	public void pause() {
		mUploadController.pause();
	}

	@Override
	public void cancel() {
		mUploadController.cancel();
	}
}
