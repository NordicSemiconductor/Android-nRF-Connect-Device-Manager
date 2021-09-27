/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.ble.exception;

import io.runtime.mcumgr.exception.McuMgrException;

/**
 * The exception is thrown when the target BLE device does not
 * support Mcu Manager (does not have required GATT service).
 */
public class McuMgrNotSupportedException extends McuMgrException {
}
