/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.ble.exception;

import no.nordicsemi.android.mcumgr.exception.McuMgrException;

/**
 * The exception is thrown when a request could not be completed due to
 * a disconnection of the target BLE device with Mcu Manager.
 */
public class McuMgrDisconnectedException extends McuMgrException {
}
