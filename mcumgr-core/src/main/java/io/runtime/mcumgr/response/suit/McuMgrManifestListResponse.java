/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.suit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.managers.SUITManager;
import io.runtime.mcumgr.response.McuMgrResponse;

/** @noinspection unused*/
public class McuMgrManifestListResponse extends McuMgrResponse {

    public static class Manifest {
        /**
         * The manifest role.
         *
         * @see KnownRole
         */
        @JsonProperty("role")
        public int role;
    }

    /**
     * List of manifests available on the device. Each manifest is identified by its role.
     * Use {@link SUITManager#getManifestState(int, McuMgrCallback)} to
     * get more manifest details.
     */
    @JsonProperty("manifests")
    public List<Manifest> manifests;

    @JsonCreator
    public McuMgrManifestListResponse() {}
}
