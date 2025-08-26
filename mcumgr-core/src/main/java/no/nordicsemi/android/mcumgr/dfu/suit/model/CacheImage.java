package no.nordicsemi.android.mcumgr.dfu.suit.model;

import org.jetbrains.annotations.NotNull;

/** @noinspection unused*/
public class CacheImage {

    /** Target partition ID. */
    public final int partitionId;

    /**
     * The image.
     */
    public final byte @NotNull [] image;

    /**
     * A wrapper for a partition cache raw image and the ID of the partition.
     *
     * @param partition the partition ID.
     * @param data the signed binary to be sent.
     */
    public CacheImage(int partition, byte @NotNull [] data) {
        this.partitionId = partition;
        this.image = data;
    }
}
