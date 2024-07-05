package io.runtime.mcumgr.dfu.mcuboot.model;

import android.util.Pair;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import io.runtime.mcumgr.exception.McuMgrException;

/** @noinspection unused*/
public class ImageSet {
    @NotNull
    private final List<TargetImage> images;

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

    public ImageSet add(TargetImage binary) {
        images.add(binary);
        return this;
    }

    public ImageSet add(byte[] image) throws McuMgrException {
        images.add(new TargetImage(image));
        return this;
    }

    public ImageSet add(Pair<Integer, byte[]> image) throws McuMgrException {
        images.add(new TargetImage(image.first, image.second));
        return this;
    }

    public ImageSet add(List<android.util.Pair<Integer, byte[]>> images) throws McuMgrException {
        for (Pair<Integer, byte[]> image : images)
            this.images.add(new TargetImage(image.first, image.second));
        return this;
    }

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
