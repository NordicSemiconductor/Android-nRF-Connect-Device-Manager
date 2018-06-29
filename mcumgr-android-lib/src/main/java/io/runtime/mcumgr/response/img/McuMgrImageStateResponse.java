/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.runtime.mcumgr.response.img;

import io.runtime.mcumgr.response.McuMgrResponse;

@SuppressWarnings("unused")
public class McuMgrImageStateResponse extends McuMgrResponse {
    // For Mynewt see:
    // https://github.com/apache/mynewt-core/blob/master/boot/split/include/split/split.h
    public static final int SPLIT_STATUS_INVALID = 0;
    public static final int SPLIT_STATUS_NOT_MATCHING = 1;
    public static final int SPLIT_STATUS_MATCHING = 2;

    /** Image slot information. */
    public ImageSlot[] images;
    /**
     * The split status. For Zephyr implementation this is always 0.
     * For Mynewt, see SPLIT_STATUS_* constants.
     */
    public int splitStatus;

    /**
     * The single image slot data structure.
     */
    public static class ImageSlot {
        /** The slot number: 0 or 1. */
        public int slot;
        /** The image version string. */
        public String version;
        /** The image hash. */
        public byte[] hash;
        /** An image is bootable when the Boot Loader verified that the image is valid. */
        public boolean bootable;
        /**
         * An image is pending when it was scheduled to be swapped to slot 0.
         * That is, after the <i>test</i> or <i>confirm</i> commands were sent, but before
         * the device was reset.
         */
        public boolean pending;
        /** An image is confirmed when it managed to boot on slot 0. */
        public boolean confirmed;
        /** An image is active when it is running on slot 0. */
        public boolean active;
        /** An image is permanent after it was confirmed using <i>confirm</i> command. */
        public boolean permanent;
    }
}
