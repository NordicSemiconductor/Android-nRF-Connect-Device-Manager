/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.dflt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class McuMgrEchoResponse extends McuMgrOsResponse {
    /** The echo response. */
    @JsonProperty("r")
    public String r;

    @JsonCreator
    public McuMgrEchoResponse() {}
}
