/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr;

public enum McuMgrScheme {
    BLE,
    COAP_BLE,
    UDP,
    COAP_UDP;

    public boolean isCoap() {
        return this == COAP_BLE || this == COAP_UDP;
    }
}
