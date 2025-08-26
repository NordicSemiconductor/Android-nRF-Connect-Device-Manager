/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.response.suit;

import com.fasterxml.jackson.annotation.JsonCreator;

import no.nordicsemi.android.mcumgr.response.UploadResponse;

public class McuMgrUploadResponse extends UploadResponse {

    @JsonCreator
    public McuMgrUploadResponse() {}
}
