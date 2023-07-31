/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.dflt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class McuMgrReadDateTimeResponse extends McuMgrOsResponse {
    /**
     * Date & time in <code>yyyy-MM-dd'T'HH:mm:ss.SSSSSS</code> format.
     */
    @JsonProperty("datetime")
    public String datetime;

    @JsonCreator
    public McuMgrReadDateTimeResponse() {}
}
