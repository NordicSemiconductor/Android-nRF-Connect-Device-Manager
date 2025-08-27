/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.response.stat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nordicsemi.android.mcumgr.managers.StatsManager;
import no.nordicsemi.android.mcumgr.response.McuMgrResponse;

public class McuMgrStatListResponse extends McuMgrResponse implements StatsManager.Response {
    /** A list of modules. */
    @JsonProperty("stat_list")
    public String[] stat_list;

    @JsonCreator
    public McuMgrStatListResponse() {}
}
