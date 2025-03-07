/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.runtime.mcumgr.response.img;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.runtime.mcumgr.managers.ImageManager;
import io.runtime.mcumgr.response.McuMgrResponse;

@SuppressWarnings("unused")
public class McuMgrImageStateResponse extends McuMgrResponse implements ImageManager.Response {
    // For Mynewt see:
    // https://github.com/apache/mynewt-core/blob/master/boot/split/include/split/split.h
    public static final int SPLIT_STATUS_INVALID = 0;
    public static final int SPLIT_STATUS_NOT_MATCHING = 1;
    public static final int SPLIT_STATUS_MATCHING = 2;

    /** Image slot information. */
    @JsonProperty("images")
    public ImageSlot[] images;
    /**
     * The split status. For Zephyr implementation this is always 0.
     * For Mynewt, see SPLIT_STATUS_* constants.
     */
    @JsonProperty("splitStatus")
    public int splitStatus;

    @JsonCreator
    public McuMgrImageStateResponse() {}

    /**
     * The single image slot data structure.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageSlot {
        /**
         * The image number used for multi-core devices.
         * The main core image has index 0, second core image is 1, etc.
         * E.g. nRF5340 has 2 cores: application core (0) and network core (1).
         * For single core devices the value is 0 (default).
         */
        @JsonProperty("image")
        public int image;
        /** The slot number: 0 or 1. */
        @JsonProperty("slot")
        public int slot;
        /** The image version string. */
        @JsonProperty("version")
        public String version;
        /** The image hash. */
        @JsonProperty("hash")
        public byte[] hash;
        /** An image is bootable when the Boot Loader verified that the image is valid. */
        @JsonProperty("bootable")
        public boolean bootable;
        /**
         * An image is pending when it was scheduled to be swapped to slot 0.
         * That is, after the <i>test</i> or <i>confirm</i> commands were sent, but before
         * the device was reset.
         */
        @JsonProperty("pending")
        public boolean pending;
        /** An image is confirmed when it managed to boot from and was confirmed. */
        @JsonProperty("confirmed")
        public boolean confirmed;
        /** An image is active when it is running. */
        @JsonProperty("active")
        public boolean active;
        /** An image is permanent after it was confirmed using <i>confirm</i> command. */
        @JsonProperty("permanent")
        public boolean permanent;
        /** A flag indicating whether the image is compressed. */
        @JsonProperty("compressed")
        public boolean compressed;

        @JsonCreator
        public ImageSlot() {}
    }
}
