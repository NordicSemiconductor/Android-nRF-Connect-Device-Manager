/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.ble;

import android.bluetooth.BluetoothDevice;

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
    public void onDeviceConnecting(BluetoothDevice device) {
    }

    @Override
    public void onDeviceConnected(BluetoothDevice device) {
    }

    @Override
    public void onDeviceDisconnecting(BluetoothDevice device) {
    }

    @Override
    public void onDeviceDisconnected(BluetoothDevice device) {
    }

    @Override
    public void onLinkLossOccurred(BluetoothDevice device) {
    }

    @Override
    public void onServicesDiscovered(BluetoothDevice device, boolean optionalServicesFound) {
    }

    @Override
    public void onDeviceReady(BluetoothDevice device) {
    }

    @Override
    public boolean shouldEnableBatteryLevelNotifications(BluetoothDevice device) {
        return false;
    }

    @Override
    public void onBatteryValueReceived(BluetoothDevice device, int value) {
    }

    @Override
    public void onBondingRequired(BluetoothDevice device) {
    }

    @Override
    public void onBondingFailed(BluetoothDevice device) {
    }

    @Override
    public void onBonded(BluetoothDevice device) {
    }

    @Override
    public void onError(BluetoothDevice device, String message, int errorCode) {
    }

    @Override
    public void onDeviceNotSupported(BluetoothDevice device) {
    }
}
