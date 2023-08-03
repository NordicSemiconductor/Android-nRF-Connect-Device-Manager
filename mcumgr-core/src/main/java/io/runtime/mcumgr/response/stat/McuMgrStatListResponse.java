/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.stat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.runtime.mcumgr.managers.StatsManager;
import io.runtime.mcumgr.response.McuMgrResponse;

public class McuMgrStatListResponse extends McuMgrResponse implements StatsManager.Response {
    /** A list of modules. */
    @JsonProperty("stat_list")
    public String[] stat_list;

    @JsonCreator
    public McuMgrStatListResponse() {}
}
