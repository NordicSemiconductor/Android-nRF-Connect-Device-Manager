/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.response.log;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nordicsemi.android.mcumgr.response.McuMgrResponse;

public class McuMgrLogListResponse extends McuMgrResponse {
    @JsonProperty("log_list")
    public String[] log_list;

    @JsonCreator
    public McuMgrLogListResponse() {}
}
