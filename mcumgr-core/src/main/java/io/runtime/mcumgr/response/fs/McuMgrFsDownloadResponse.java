/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.fs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.runtime.mcumgr.response.McuMgrResponse;

public class McuMgrFsDownloadResponse extends McuMgrResponse {
    /** The offset. Number of bytes that were sent. */
    @JsonProperty("off")
    public int off;
    /** The length of the file (in bytes). Set only in the first packet. */
    @JsonProperty("len")
    public int len;
    /** The chunk data. */
    @JsonProperty("data")
    public byte[] data;

    @JsonCreator
    public McuMgrFsDownloadResponse() {}
}
