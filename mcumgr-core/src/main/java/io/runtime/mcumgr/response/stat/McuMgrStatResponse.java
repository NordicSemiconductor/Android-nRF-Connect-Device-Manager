/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.stat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

import io.runtime.mcumgr.response.McuMgrResponse;

public class McuMgrStatResponse extends McuMgrResponse {
    /** Module name. */
    @JsonProperty("name")
    public String name;
    /** A map of key-value pairs for the given module. */
    @JsonProperty("fields")
    public Map<String, Long> fields;

    @JsonCreator
    public McuMgrStatResponse() {}
}
