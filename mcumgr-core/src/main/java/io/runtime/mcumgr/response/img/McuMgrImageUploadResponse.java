/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.img;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.jetbrains.annotations.Nullable;

import io.runtime.mcumgr.response.UploadResponse;

public class McuMgrImageUploadResponse extends UploadResponse {

    /**
     * Since NCS 2.4 the SMP server expects 32-byte 'sha' of the image sent with the
     * first data packet. After the upload is complete this variable will contain information
     * whether the SHA=256 matched the received image.
     * <p>
     * Client may abort when match is false instead of trying to confirm. This is only to ensure the
     * full file was sent correctly, not to validate the file or its signature.
     */
    @JsonProperty("match")
    @Nullable
    public Boolean match;

    @JsonCreator
    public McuMgrImageUploadResponse() {}
}
