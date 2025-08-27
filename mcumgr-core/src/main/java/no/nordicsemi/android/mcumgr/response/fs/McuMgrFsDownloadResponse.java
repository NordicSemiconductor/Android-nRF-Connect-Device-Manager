/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.response.fs;

import com.fasterxml.jackson.annotation.JsonCreator;

import no.nordicsemi.android.mcumgr.managers.FsManager;
import no.nordicsemi.android.mcumgr.response.DownloadResponse;

public class McuMgrFsDownloadResponse extends DownloadResponse implements FsManager.Response {
    @JsonCreator
    public McuMgrFsDownloadResponse() {}
}
