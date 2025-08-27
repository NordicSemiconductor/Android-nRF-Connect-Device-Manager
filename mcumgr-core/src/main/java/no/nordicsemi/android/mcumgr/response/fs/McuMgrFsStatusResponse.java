/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.response.fs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nordicsemi.android.mcumgr.managers.FsManager;
import no.nordicsemi.android.mcumgr.response.McuMgrResponse;

public class McuMgrFsStatusResponse extends McuMgrResponse implements FsManager.Response {
    /** Length of file (in bytes). */
    @JsonProperty("len")
    public int len;

    @JsonCreator
    public McuMgrFsStatusResponse() {}
}
