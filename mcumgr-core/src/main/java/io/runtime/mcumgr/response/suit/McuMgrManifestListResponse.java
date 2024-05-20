/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.suit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.jetbrains.annotations.Nullable;

import java.util.List;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.managers.SUITManager;
import io.runtime.mcumgr.response.McuMgrResponse;

/** @noinspection unused*/
public class McuMgrManifestListResponse extends McuMgrResponse {

    public static class Manifest {

        /**
         * Known manifest roles.
         * This enum is based on <a href="https://github.com/nrfconnect/sdk-nrf/blob/46f6922cce98c9b9ccb69c2458bf57f9bcfb85b9/subsys/suit/metadata/include/suit_metadata.h#L70-L98">suit_metadata.h</a>.
         */
        public enum KnownRole {
            /** Manifest describes the entry-point for all Nordic-controlled manifests. */
            SEC_TOP(0x10),
            /** Manifest describes SDFW firmware and recovery updates. */
            SEC_SDFW(0x11),
            /** Manifest describes SYSCTRL firmware update and boot procedures. */
            SEC_SYSCTRL(0x12),

            /** Manifest describes the entry-point for all OEM-controlled manifests. */
            APP_ROOT(0x20),
            /** Manifest describes OEM-specific recovery procedure. */
            APP_RECOVERY(0x21),
            /** Manifest describes OEM-specific binaries, specific for application core. */
            APP_LOCAL_1(0x22),
            /** Manifest describes OEM-specific binaries, specific for application core. */
            APP_LOCAL_2(0x23),
            /** Manifest describes OEM-specific binaries, specific for application core. */
            APP_LOCAL_3(0x24),

            /** Manifest describes radio part of OEM-specific recovery procedure. */
            RAD_RECOVERY(0x30),
            /** Manifest describes OEM-specific binaries, specific for radio core. */
            RAD_LOCAL_1(0x31),
            /** Manifest describes OEM-specific binaries, specific for radio core. */
            RAD_LOCAL_2(0x32);

            /** The role value. */
            public final int id;
            
            KnownRole(int id) {
                this.id = id;
            }
        }

        @JsonProperty("role")
        public int role;

        /**
         * Returns the role as a {@link KnownRole} enum, or null if the role is unknown.
         */
        @Nullable
        public KnownRole getRoleOrNull() {
            for (KnownRole knownRole : KnownRole.values()) {
                if (knownRole.id == role) {
                    return knownRole;
                }
            }
            return null;
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
