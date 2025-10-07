package no.nordicsemi.android.mcumgr.dfu.mcuboot.model;

import org.jetbrains.annotations.NotNull;

import no.nordicsemi.android.mcumgr.exception.McuMgrException;
import no.nordicsemi.android.mcumgr.image.ImageWithHash;
import no.nordicsemi.android.mcumgr.image.McuMgrImage;
import no.nordicsemi.android.mcumgr.image.SUITImage;

/** @noinspection unused*/
public class TargetImage {
    public final static int SLOT_PRIMARY = 0;
    public final static int SLOT_SECONDARY = 1;

    /** Target image index (core index) for the image. */
    public final int imageIndex;
    /**
     *  Target slot for the image. By default images are run from the primary slot.
     *  An update will be written to the secondary slot and will be swapped to the primary slot
     *  after it is confirmed and reset.
     *  <p>
     *  If the device supports Direct XIP feature it is possible to run an app from a secondary
     *  slot. The image has to be compiled for this slot. A ZIP package in that case may
     *  contain both images. In that case only the one compiled for the available slot will be sent,
     *  there is no swapping and the image is run directly from the slot that it was sent to.
     */
    public final int slot;
    /**
     * The image.
     * <p>
     * Currently only MCUboot images are supported. Valid images contain a header with a MAGIC
     * number and a version number.
     */
    public final ImageWithHash image;

    /**
     * This constructor creates a basic image target. It will be sent to the secondary slot (slot = 1)
     * of the default core (image index = 0).
     * <p>
     * This constructor can be used to send an update to a single core device that does not support
     * Direct XIP (option to boot an image from a non-primary image).
     * @param data the signed binary to be sent.
     * @throws McuMgrException when the image does not have a valid mcu header
     */
    public TargetImage(byte @NotNull [] data) throws McuMgrException {
        this(
            // Default or single core.
            0,
            // If not specified, the image will be sent to the secondary slot.
            SLOT_SECONDARY,
            data
        );
    }

    /**
     * This constructor creates a basic image targeting specified core (image index). The binary will
     * be sent to the secondary slot (slot = 1) for that core.
     * <p>
     * This constructor can be used to send an update to a multi-core device that does not support
     * Direct XIP (option to boot an image from a non-primary image).
     * @param imageIndex an index of the core (0 is the main (app) core, 1 is secondary (network) core, etc.
     * @param data the signed binary to be sent.
     * @throws McuMgrException when the image does not have a valid mcu header
     */
    public TargetImage(int imageIndex, byte @NotNull [] data) throws McuMgrException {
        this(
            imageIndex,
            // If not specified, the image will be sent to the secondary slot.
            SLOT_SECONDARY,
            data
        );
    }

    /**
     * This constructor allows to define a target for a multi-core device with support for Direct XIP
     * (option to run an image from a noo-primary slot. In that case it should contain binaries
     * compiled for any slot and the DFU uploader will determine the correct slot when validating the
     * device.
     * @param imageIndex an index of the core (0 is the main (app) core, 1 is secondary (network) core, etc.
     * @param slot 0 for a primary slot and 1 for a secondary slot.
     * @param data the signed binary to be sent.
     * @throws McuMgrException when the image does not have a valid mcu header
     */
    public TargetImage(int imageIndex, int slot, byte @NotNull [] data) throws McuMgrException {
        this.imageIndex = imageIndex;
        this.slot = slot;
        ImageWithHash tmp;
        try {
            tmp = McuMgrImage.fromBytes(data);
        } catch (McuMgrException e) {
            try {
                tmp = SUITImage.fromBytes(data);
            } catch (McuMgrException e2) {
                throw new McuMgrException("The image does not have a valid mcu header");
            }
        }
        this.image = tmp;
    }
}
