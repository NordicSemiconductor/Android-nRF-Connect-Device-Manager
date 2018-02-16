/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.resp;

public interface McuMgrResponse {

    /**
     * Used primarily for a CoAP schemes to indicate a CoAP response error.
     *
     * @return true if a Mcu Manager response was received successfully (i.e. no CoAP error), false
     * otherwise.
     */
    boolean isSuccess();
}
