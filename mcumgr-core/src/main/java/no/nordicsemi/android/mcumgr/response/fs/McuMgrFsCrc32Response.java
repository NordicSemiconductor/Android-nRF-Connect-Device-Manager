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

public class McuMgrFsCrc32Response extends McuMgrResponse implements FsManager.Response {
    /** Type of hash/checksum that was performed. */
    @JsonProperty("type")
    public String type;

    /** Offset that checksum calculation started at. */
    @JsonProperty("off")
    public int off;

    /** Length of input data used for hash/checksum generation (in bytes). */
    @JsonProperty("len")
    public int len;

    /** IEEE CRC32 checksum (uint32 represented as type long). */
    @JsonProperty("output")
    public long output;

    @JsonCreator
    public McuMgrFsCrc32Response() {}
}
