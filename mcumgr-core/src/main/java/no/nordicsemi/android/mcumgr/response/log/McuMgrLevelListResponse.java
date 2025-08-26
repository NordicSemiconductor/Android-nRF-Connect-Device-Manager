/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.response.log;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nordicsemi.android.mcumgr.response.McuMgrResponse;

public class McuMgrLevelListResponse extends McuMgrResponse {
    @JsonProperty("level_map")
    public String[] level_map;

    @JsonCreator
    public McuMgrLevelListResponse() {}
}
