package io.runtime.mcumgr.dfu.suit.task;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import io.runtime.mcumgr.dfu.suit.SUITUpgradeManager;
import io.runtime.mcumgr.dfu.suit.SUITUpgradePerformer;
import io.runtime.mcumgr.dfu.suit.model.CacheImage;
import io.runtime.mcumgr.dfu.suit.model.CacheImageSet;
import io.runtime.mcumgr.task.TaskManager;

/**
 * This task performs the DFU using SUIT manager.
 */
public class PerformDfu extends SUITUpgradeTask {

	private final byte @NotNull [] envelope;
	private final CacheImageSet cacheImages;

	/**
	 * Create a new PerformDfu task.
	 * @param envelope the SUIT candidate envelope.
	 * @noinspection unused
	 */
	public PerformDfu(final byte @NotNull [] envelope) {
		this.envelope = envelope;
		this.cacheImages = null;
	}

	/**
	 * Create a new PerformDfu task.
	 * @param envelope the SUIT candidate envelope.
	 * @param cacheImages cache images to be uploaded before starting the update.
	 */
	public PerformDfu(final byte @NotNull [] envelope,
					  final @Nullable CacheImageSet cacheImages) {
		this.envelope = envelope;
		this.cacheImages = cacheImages;
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
		// Upload the candidate envelope.
		performer.enqueue(new UploadEnvelope(envelope, cacheImages != null));

		// Upload the cache images, if any.
		if (cacheImages != null) {
			final List<CacheImage> images = cacheImages.getImages();
			for (CacheImage image : images) {
				performer.enqueue(new UploadCache(image.partitionId, image.image));
			}
			// After the cache images are uploaded, begin the deferred install.
			performer.enqueue(new BeginInstall());
		}

		// After the candidate envelope and cache images are uploaded, client should poll
		// for more resources.
		performer.enqueue(new PollTask());

		// Enqueuing is done, notify the performer that this task is complete.
		performer.onTaskCompleted(this);
	}
}
