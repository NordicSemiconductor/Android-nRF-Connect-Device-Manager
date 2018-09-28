/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.log;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

import io.runtime.mcumgr.response.McuMgrResponse;

public class McuMgrModuleListResponse extends McuMgrResponse {
    @JsonProperty("module_map")
    public Map<String, Integer> module_map;

    @JsonCreator
    public McuMgrModuleListResponse() {}
}
