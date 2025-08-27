/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.response.stat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

import no.nordicsemi.android.mcumgr.managers.StatsManager;
import no.nordicsemi.android.mcumgr.response.McuMgrResponse;

public class McuMgrStatResponse extends McuMgrResponse implements StatsManager.Response {
    /** Module name. */
    @JsonProperty("name")
    public String name;
    /** A map of key-value pairs for the given module. */
    @JsonProperty("fields")
    public Map<String, Long> fields;

    @JsonCreator
    public McuMgrStatResponse() {}
}
