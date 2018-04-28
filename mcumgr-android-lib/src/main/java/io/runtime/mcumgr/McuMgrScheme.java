/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr;

/**
 * The MCU Manager protocol can be used over a variety of transports and schemes. On android phones,
 * the only applicable schemes are {@link McuMgrScheme#BLE}, {@link McuMgrScheme#COAP_BLE}, and
 * {@link McuMgrScheme#COAP_UDP}. Besides the obvious transport differences, the scheme primarily
 * determines the format of the MCU Manager packet. For example, CoAP schemes put the 8-byte MCU
 * Manager header as a key-value pair in the CBOR payload while non-CoAP schemes simply append the
 * CBOR payload after the 8-byte header.
 */
public enum McuMgrScheme {
    BLE,
    COAP_BLE,
    COAP_UDP;

    public boolean isCoap() {
        return this == COAP_BLE || this == COAP_UDP;
    }
}
