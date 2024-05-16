/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.suit;

import com.fasterxml.jackson.annotation.JsonCreator;

import io.runtime.mcumgr.response.UploadResponse;

public class McuMgrEnvelopeUploadResponse extends UploadResponse {

    @JsonCreator
    public McuMgrEnvelopeUploadResponse() {}
}
