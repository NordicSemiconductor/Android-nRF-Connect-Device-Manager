/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.ble.exception;

import no.nordicsemi.android.mcumgr.exception.McuMgrException;

/**
 * The exception is thrown when the target BLE device does not
 * support Mcu Manager (does not have required GATT service).
 */
public class McuMgrNotSupportedException extends McuMgrException {
}
