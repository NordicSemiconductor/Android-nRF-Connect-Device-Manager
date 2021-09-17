/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.img;

import com.fasterxml.jackson.annotation.JsonCreator;

import io.runtime.mcumgr.response.UploadResponse;

public class McuMgrImageUploadResponse extends UploadResponse {
    @JsonCreator
    public McuMgrImageUploadResponse() {}
}
