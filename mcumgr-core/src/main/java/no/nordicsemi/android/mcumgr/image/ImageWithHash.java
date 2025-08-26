package no.nordicsemi.android.mcumgr.image;

import org.jetbrains.annotations.NotNull;

public interface ImageWithHash {
    /** Returns the image data. */
    byte @NotNull [] getData();
    /** Returns the hash of the image. */
    byte @NotNull [] getHash();
    /** Returns true if the image needs confirmation to be applied. */
    boolean needsConfirmation();
}
