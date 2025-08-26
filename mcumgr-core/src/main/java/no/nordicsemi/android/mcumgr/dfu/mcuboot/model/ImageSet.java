package no.nordicsemi.android.mcumgr.dfu.mcuboot.model;

import android.util.Pair;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import no.nordicsemi.android.mcumgr.dfu.suit.model.CacheImage;
import no.nordicsemi.android.mcumgr.exception.McuMgrException;

/**
 * Represents a set of images to be sent to the device using teh Image group (manager).
 * <p>
 * The Image manager can be used to update devices with MCUboot and SUIT bootloaders.
 * For SUIT bootloaders a dedicated SUIT manager should be used, but some devices support both
 * or only Image manager (e.g. for recovery).
 * @noinspection unused
 */
public class ImageSet {
    /**
     * List of target images to be sent to the device.
     * <p>
     * A device with MCUboot bootloader can have multiple images. Each image is identified by
     * an image index. Images must be sent with the "image" parameter set to the image index.
     * When all images are sent the client should send "test" or "confirm" command to confirm them
     * and "reset" command to begin the update.
     */
    @NotNull
    private final List<TargetImage> images;

    /**
     * Cache images are used to update devices supporting SUIT manifest. The cache images are
     * sent after the SUIT manifest and contain parts of firmware that are not included in the
     * manifest.
     * <p>
     * In case the cache images are not null, {@link #images} must contain a single SUIT file.
     * <p>
     * Flow:
     * 1. Send .suit file ("image" is set to 0 (default))
     * 2. Send cache images, each with "image" parameter set to the partition ID.
     * 3. Send "confirm" command (without hash) to begin the update.
     */
    @Nullable
    private List<CacheImage> cacheImages;

    /**
     * Creates an empty image set. Use {@link #add(TargetImage)} to add targets.
     */
    public ImageSet() {
        this.images = new ArrayList<>(4);
    }

    /**
     * Creates an image set with given targets.
     * @param targets image targets.
     */
    public ImageSet(@NotNull final List<TargetImage> targets) {
        this.images = targets;
    }

    /**
     * Returns list of targets.
     */
    @NotNull
    public List<TargetImage> getImages() {
        return images;
    }

    @Nullable
    public List<CacheImage> getCacheImages() {
        return cacheImages;
    }

    @NotNull
    public ImageSet add(TargetImage binary) {
        images.add(binary);
        return this;
    }

    @NotNull
    public ImageSet add(CacheImage cacheImage) {
        if (cacheImages == null) {
            cacheImages = new ArrayList<>();
        }
        cacheImages.add(cacheImage);
        return this;
    }

    @NotNull
    public ImageSet set(List<CacheImage> cacheImages) {
        this.cacheImages = cacheImages;
        return this;
    }

    @NotNull
    public ImageSet add(byte[] image) throws McuMgrException {
        images.add(new TargetImage(image));
        return this;
    }

    @NotNull
    public ImageSet add(Pair<Integer, byte[]> image) throws McuMgrException {
        images.add(new TargetImage(image.first, image.second));
        return this;
    }

    @NotNull
    public ImageSet add(List<android.util.Pair<Integer, byte[]>> images) throws McuMgrException {
        for (Pair<Integer, byte[]> image : images)
            this.images.add(new TargetImage(image.first, image.second));
        return this;
    }

    @NotNull
    public ImageSet removeImagesWithImageIndex(int imageIndex) {
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).imageIndex == imageIndex) {
                images.remove(i);
                i--;
            }
        }
        return this;
    }
}
