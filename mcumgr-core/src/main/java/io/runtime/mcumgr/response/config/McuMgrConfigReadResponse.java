/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.runtime.mcumgr.response.McuMgrResponse;

public class McuMgrConfigReadResponse extends McuMgrResponse {
    /** The value of the config variable. */
    @JsonProperty("val")
    public String val;

    @JsonCreator
    public McuMgrConfigReadResponse() {}
}
