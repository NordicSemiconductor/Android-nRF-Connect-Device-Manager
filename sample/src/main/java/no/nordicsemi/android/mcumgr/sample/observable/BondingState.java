/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.observable;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

public enum BondingState {
    NOT_BONDED,
    BONDING,
    BONDED;

    static BondingState of(@NonNull final BluetoothDevice device) {
        switch (device.getBondState()) {
            case BluetoothDevice.BOND_BONDING:
                return BONDING;
            case BluetoothDevice.BOND_BONDED:
                return BONDED;
            case BluetoothDevice.BOND_NONE:
            default:
                return NOT_BONDED;
        }
    }
}
