/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.response.dflt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class McuMgrParamsResponse extends McuMgrOsResponse {
    /** The McuMgr buffer size. */
    @JsonProperty("buf_size")
    public int bufSize;

    /** Number of McuMgr buffers. */
    @JsonProperty("buf_count")
    public int bufCount;

    @JsonCreator
    public McuMgrParamsResponse() {}
}
