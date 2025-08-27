/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.response.dflt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nordicsemi.android.mcumgr.managers.DefaultManager;

/** @noinspection unused*/
public class McuMgrBootloaderInfoResponse extends McuMgrOsResponse {
    // MCUboot modes are explained here: https://docs.mcuboot.com/design.html#image-slots

    /** Unknown mode of MCUboot. */
    public static final int MODE_UNKNOWN = -1;
    /** MCUboot is in single application mode. */
    public static final int MODE_SINGLE_APP = 0;
    /** MCUboot is in swap using scratch partition mode. */
    public static final int MODE_SWAP_SCRATCH = 1;
    /** MCUboot is in overwrite (upgrade-only) mode. */
    public static final int MODE_OVERWRITE_ONLY = 2;
    /** MCUboot is in swap without scratch mode. */
    public static final int MODE_SWAP_WITHOUT_SCRATCH = 3;
    /** MCUboot is in DirectXIP without revert mode. */
    public static final int MODE_DIRECT_XIP	= 4;
    /** MCUboot is in DirectXIP with revert mode. */
    public static final int MODE_DIRECT_XIP_WITH_REVERT	= 5;
    /** MCUboot is in RAM loader mode. */
    public static final int MODE_RAM_LOADER = 6;
    /** MCUboot is in Firmware Loader mode. */
    public static final int MODE_FIRMWARE_LOADER = 7;

    // Note: other modes may be added in the future.

    /** Text response including requested parameters. */
    @JsonProperty("mode")
    public int mode = MODE_UNKNOWN;

    /**
     * The "no-downgrade" is a flag, indicating that mode has downgrade prevention enabled;
     * downgrade prevention means that if uploaded image has lower version than running,
     * it will not be taken for execution by MCUboot.
     * MCUmgr may reject image with lower version in that MCUboot configuration.
     */
    @JsonProperty("no-downgrade")
    public boolean noDowngrade = false;

    @JsonProperty("bootloader")
    public String bootloader;

    /** The active slot number is unknown. */
    public static final int UNKNOWN_SLOT = -1;

    /**
     * The id of the active B0 slot. It can be either 0 (primary) or
     * 1 (secondary). This is set only when the request was made with
     * {@link DefaultManager#BOOTLOADER_INFO_QUERY_ACTIVE_B0_SLOT}
     * parameter, otherwise set to {@value #UNKNOWN_SLOT}.
     */
    @JsonProperty("active")
    public int activeB0Slot = UNKNOWN_SLOT;

    @JsonCreator
    public McuMgrBootloaderInfoResponse() {}
}
