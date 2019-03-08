/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.fs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.runtime.mcumgr.response.DownloadResponse;
import io.runtime.mcumgr.response.McuMgrResponse;

public class McuMgrFsDownloadResponse extends DownloadResponse {
    @JsonCreator
    public McuMgrFsDownloadResponse() {}
}
