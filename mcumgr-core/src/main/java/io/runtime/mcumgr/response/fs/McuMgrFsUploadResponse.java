/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.fs;

import com.fasterxml.jackson.annotation.JsonCreator;

import io.runtime.mcumgr.managers.FsManager;
import io.runtime.mcumgr.response.UploadResponse;

public class McuMgrFsUploadResponse extends UploadResponse implements FsManager.Response {
    @JsonCreator
    public McuMgrFsUploadResponse() {}
}
