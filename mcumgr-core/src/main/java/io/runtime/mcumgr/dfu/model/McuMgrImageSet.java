package io.runtime.mcumgr.dfu.model;

import android.util.Pair;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import io.runtime.mcumgr.exception.McuMgrException;

/** @noinspection unused*/
public class McuMgrImageSet {
    @NotNull
    private final List<McuMgrTargetImage> images;

    /**
     * Creates an empty image set. Use {@link #add(McuMgrTargetImage)} to add targets.
     */
    public McuMgrImageSet() {
        this.images = new ArrayList<>(4);
    }

    /**
     * Creates an image set with given targets.
     * @param targets image targets.
     */
    public McuMgrImageSet(@NotNull final List<McuMgrTargetImage> targets) {
        this.images = targets;
    }

    /**
     * Returns list of targets.
     */
    @NotNull
    public List<McuMgrTargetImage> getImages() {
        return images;
    }

    public McuMgrImageSet add(McuMgrTargetImage binary) {
        images.add(binary);
        return this;
    }

    public McuMgrImageSet add(byte[] image) throws McuMgrException {
        images.add(new McuMgrTargetImage(image));
        return this;
    }

    public McuMgrImageSet add(Pair<Integer, byte[]> image) throws McuMgrException {
        images.add(new McuMgrTargetImage(image.first, image.second));
        return this;
    }

    public McuMgrImageSet add(List<android.util.Pair<Integer, byte[]>> images) throws McuMgrException {
        for (Pair<Integer, byte[]> image : images)
            this.images.add(new McuMgrTargetImage(image.first, image.second));
        return this;
    }

    public McuMgrImageSet removeImagesWithImageIndex(int imageIndex) {
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).imageIndex == imageIndex) {
                images.remove(i);
                i--;
            }
        }
        return this;
    }
}
