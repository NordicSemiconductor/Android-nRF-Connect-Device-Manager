/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.dflt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** @noinspection unused*/
@JsonIgnoreProperties(ignoreUnknown = true)
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

    @JsonCreator
    public McuMgrBootloaderInfoResponse() {}
}
