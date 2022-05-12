/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.dflt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.runtime.mcumgr.response.McuMgrResponse;

public class McuMgrParamsResponse extends McuMgrResponse {
    /** The McuMgr buffer size. */
    @JsonProperty("buf_size")
    public int bufSize;

    /** Number of McuMgr buffers. */
    @JsonProperty("buf_count")
    public int bufCount;

    @JsonCreator
    public McuMgrParamsResponse() {}
}
