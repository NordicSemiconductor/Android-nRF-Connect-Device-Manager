package no.nordicsemi.android.mcumgr.dfu.mcuboot.task;

import org.jetbrains.annotations.NotNull;

import no.nordicsemi.android.mcumgr.dfu.mcuboot.FirmwareUpgradeManager.Settings;
import no.nordicsemi.android.mcumgr.dfu.mcuboot.FirmwareUpgradeManager.State;
import no.nordicsemi.android.mcumgr.exception.McuMgrException;
import no.nordicsemi.android.mcumgr.managers.ImageManager;
import no.nordicsemi.android.mcumgr.task.TaskManager;
import no.nordicsemi.android.mcumgr.transfer.ImageUploader;
import no.nordicsemi.android.mcumgr.transfer.TransferController;
import no.nordicsemi.android.mcumgr.transfer.UploadCallback;

class Upload extends FirmwareUpgradeTask {
	private final byte[] data;
	private final int image;

	/**
	 * Upload controller used to pause, resume, and cancel upload. Set when the upload is started.
	 */
	private TransferController mUploadController;

	Upload(final byte @NotNull [] data, final int image) {
		this.data = data;
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
		final ImageManager manager = new ImageManager(performer.getTransport());
		if (settings.windowCapacity > 1) {
			mUploadController =	new ImageUploader(
					manager,
					data, image,
					settings.windowCapacity,
					settings.memoryAlignment
			).uploadAsync(callback);
		} else {
			mUploadController = manager.imageUpload(data, image, callback);
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
