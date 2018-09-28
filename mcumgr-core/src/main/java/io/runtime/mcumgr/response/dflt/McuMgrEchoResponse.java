/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.dflt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.runtime.mcumgr.response.McuMgrResponse;

public class McuMgrEchoResponse extends McuMgrResponse {
    /** The echo response. */
    @JsonProperty("r")
    public String r;

    @JsonCreator
    public McuMgrEchoResponse() {}
}
