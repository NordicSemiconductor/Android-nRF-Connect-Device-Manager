/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.fs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.runtime.mcumgr.managers.FsManager;
import io.runtime.mcumgr.response.McuMgrResponse;

public class McuMgrFsStatusResponse extends McuMgrResponse implements FsManager.Response {
    /** Length of file (in bytes). */
    @JsonProperty("len")
    public int len;

    @JsonCreator
    public McuMgrFsStatusResponse() {}
}
