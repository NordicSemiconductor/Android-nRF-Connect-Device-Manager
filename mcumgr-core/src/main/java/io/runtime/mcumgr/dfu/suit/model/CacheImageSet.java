package io.runtime.mcumgr.dfu.suit.model;

import android.util.Pair;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import io.runtime.mcumgr.exception.McuMgrException;

/** @noinspection unused*/
public class CacheImageSet {
    @NotNull
    private final List<CacheImage> images;

    /**
     * Creates an empty image set. Use {@link #add(CacheImage)} to add targets.
     */
    public CacheImageSet() {
        this.images = new ArrayList<>(4);
    }

    /**
     * Creates an image set with given targets.
     * @param targets image targets.
     */
    public CacheImageSet(@NotNull final List<CacheImage> targets) {
        this.images = targets;
    }

    /**
     * Returns list of targets.
     */
    @NotNull
    public List<CacheImage> getImages() {
        return images;
    }

    public CacheImageSet add(CacheImage image) throws McuMgrException {
        images.add(image);
        return this;
    }

    public CacheImageSet add(int partition, byte[] image) throws McuMgrException {
        images.add(new CacheImage(partition, image));
        return this;
    }

    public CacheImageSet add(Pair<Integer, byte[]> image) throws McuMgrException {
        images.add(new CacheImage(image.first, image.second));
        return this;
    }

    public CacheImageSet add(List<Pair<Integer, byte[]>> images) throws McuMgrException {
        for (Pair<Integer, byte[]> image : images)
            this.images.add(new CacheImage(image.first, image.second));
        return this;
    }
}
