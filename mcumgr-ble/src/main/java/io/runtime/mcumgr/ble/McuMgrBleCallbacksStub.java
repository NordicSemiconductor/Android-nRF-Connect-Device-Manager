/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.ble;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import no.nordicsemi.android.ble.BleManagerCallbacks;

/**
 * <b>DO NOT PERFORM ANY BLOCKING OPERATIONS INSIDE THESE CALLBACKS!</b>
 * <p>
 * Blocking these callbacks may risk the integrity of the BleManager.
 * <p>
 * Callbacks from {@link McuMgrBleTransport}.
 */
public class McuMgrBleCallbacksStub implements BleManagerCallbacks {

    @Override
    public void onDeviceConnecting(@NonNull BluetoothDevice device) {
    }

    @Override
    public void onDeviceConnected(@NonNull BluetoothDevice device) {
    }

    @Override
    public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {
    }

    @Override
    public void onDeviceDisconnected(@NonNull BluetoothDevice device) {
    }

    @Override
    public void onLinkLossOccurred(@NonNull BluetoothDevice device) {
    }

    @Override
    public void onServicesDiscovered(@NonNull BluetoothDevice device, boolean optionalServicesFound) {
    }

    @Override
    public void onDeviceReady(@NonNull BluetoothDevice device) {
    }

    @Override
    public boolean shouldEnableBatteryLevelNotifications(@NonNull BluetoothDevice device) {
        return false;
    }

    @Override
    public void onBatteryValueReceived(@NonNull BluetoothDevice device, int value) {
    }

    @Override
    public void onBondingRequired(@NonNull BluetoothDevice device) {
    }

    @Override
    public void onBondingFailed(@NonNull BluetoothDevice device) {
    }

    @Override
    public void onBonded(@NonNull BluetoothDevice device) {
    }

    @Override
    public void onError(@NonNull BluetoothDevice device, @NonNull String message, int errorCode) {
    }

    @Override
    public void onDeviceNotSupported(@NonNull BluetoothDevice device) {
    }
}
