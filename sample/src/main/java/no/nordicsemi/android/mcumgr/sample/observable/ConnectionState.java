/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.observable;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import androidx.annotation.NonNull;

public enum ConnectionState {
    CONNECTING,
    INITIALIZING,
    READY,
    DISCONNECTING,
    DISCONNECTED,
    TIMEOUT,
    NOT_SUPPORTED;

    static ConnectionState of(@NonNull final Context context,
                              @NonNull final BluetoothDevice device) {
        final BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager == null)
            return DISCONNECTED;

        final int state = manager.getConnectionState(device, BluetoothProfile.GATT);
        if (state == BluetoothProfile.STATE_CONNECTED)
            return READY;

        return DISCONNECTED;
    }
}
