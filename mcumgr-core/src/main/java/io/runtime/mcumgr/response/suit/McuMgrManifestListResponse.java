/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.suit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import io.runtime.mcumgr.response.McuMgrResponse;

public class McuMgrManifestListResponse extends McuMgrResponse {

    public static class Manifest {
        @JsonProperty("role")
        public int role;
    }

    /**
     * Manifest role encoded as two nibbles: &lt;domain ID&gt; &lt;index&gt;.
     * Some role values are already know, i.e. 0x20 - Root Manifest.
     */
    @JsonProperty("manifests")
    public List<Manifest> manifests;

    @JsonCreator
    public McuMgrManifestListResponse() {}
}
