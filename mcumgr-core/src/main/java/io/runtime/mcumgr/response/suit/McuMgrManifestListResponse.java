/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.suit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.managers.SUITManager;
import io.runtime.mcumgr.response.McuMgrResponse;

/** @noinspection unused*/
public class McuMgrManifestListResponse extends McuMgrResponse {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Manifest {
        /**
         * The manifest role.
         *
         * @see KnownRole
         */
        @JsonProperty("role")
        public int role;

        @JsonCreator
        public Manifest() {}

        @NotNull
        @Override
        public String toString() {
            final KnownRole knownRole = KnownRole.getOrNull(role);
            if (knownRole == null) {
                return String.format(Locale.US, "UNKNOWN (%02x)", role);
            }
            return knownRole.toString();
        }
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
