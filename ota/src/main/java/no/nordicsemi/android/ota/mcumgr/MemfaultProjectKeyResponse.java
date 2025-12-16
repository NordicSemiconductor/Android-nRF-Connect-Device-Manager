/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.ota.mcumgr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nordicsemi.android.mcumgr.response.McuMgrResponse;

public class MemfaultProjectKeyResponse extends McuMgrResponse implements MemfaultManager.Response {
    /** The Memfault Project Key. */
    @JsonProperty("project_key")
    public String projectKey;

    @JsonCreator
    public MemfaultProjectKeyResponse() {}
}
