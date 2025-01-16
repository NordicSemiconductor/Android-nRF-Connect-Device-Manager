/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.runtime.mcumgr.response.img;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.runtime.mcumgr.managers.ImageManager;
import io.runtime.mcumgr.response.McuMgrResponse;

@SuppressWarnings("unused")
public class McuMgrImageSlotResponse extends McuMgrResponse implements ImageManager.Response {
    /** Slot information for each image (core). */
    @JsonProperty("images")
    public Image[] images;

    /**
     * The single image data structure.
     */
    public static class Image {

        /**
         * The single slot data structure.
         */
        public static class Slot {
            /** The slot number: 0 or 1. */
            @JsonProperty("slot")
            public int slot;
            /** The slot size in bytes. */
            @JsonProperty("size")
            public int size;
            /**
             * Optional field (only present if <code>CONFIG_MCUMGR_GRP_IMG_DIRECT_UPLOAD</code> is
             * enabled to allow direct image uploads) which specifies the image ID that can be
             * used by external tools to upload an image to that slot.
             */
            @JsonProperty("upload_image_id")
            public Integer uploadImageId;

            @JsonCreator
            public Slot() {}
        }

        /**
         * The image number used for multi-core devices.
         * The main core image has index 0, second core image is 1, etc.
         * E.g. nRF5340 has 2 cores: application core (0) and network core (1).
         * For single core devices the value is 0 (default).
         */
        @JsonProperty("image")
        public int image;
        @JsonProperty("slots")
        public Slot[] slots;
        /**
         * Optional field (only present if <code>CONFIG_MCUMGR_GRP_IMG_TOO_LARGE_SYSBUILD</code> or
         * <code>CONFIG_MCUMGR_GRP_IMG_TOO_LARGE_BOOTLOADER_INFO</code> are enabled) which specifies the
         * maximum size of an application that can be uploaded to that image number.
         */
        @JsonProperty("max_image_size")
        public Integer maxImageSize;

        @JsonCreator
        public Image() {}
    }

    @JsonCreator
    public McuMgrImageSlotResponse() {}
}
