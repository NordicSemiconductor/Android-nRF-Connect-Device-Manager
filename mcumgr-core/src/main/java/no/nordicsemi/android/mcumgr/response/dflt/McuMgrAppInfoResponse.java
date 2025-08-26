/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.response.dflt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class McuMgrAppInfoResponse extends McuMgrOsResponse {
    /** Text response including requested parameters. */
    @JsonProperty("output")
    public String output;

    @JsonCreator
    public McuMgrAppInfoResponse() {}
}
