/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.response.settings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nordicsemi.android.mcumgr.response.McuMgrResponse;

public class McuMgrSettingsReadResponse extends McuMgrResponse {
    /**
     * Binary string of the returned data.
     * <p>
     * Note that the underlying data type cannot be specified through this and must be known by the client.
     */
    @JsonProperty("val")
    public byte[] val;
    /**
     * Set if maximum supported data size is smaller than the maximum requested
     * data size, and contains the maximum data size which the device supports.
     */
    @JsonProperty("max_size")
    public Integer maxSize;

    @JsonCreator
    public McuMgrSettingsReadResponse() {}
}
