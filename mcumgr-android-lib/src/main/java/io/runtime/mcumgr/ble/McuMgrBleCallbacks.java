/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import io.runtime.mcumgr.ble.manager.BleManagerCallbacks;

/**
 * <b>DO NOT PERFORM ANY BLOCKING OPERATIONS INSIDE THESE CALLBACKS!</b>
 * <p>
 * Blocking these callbacks may risk the integrity of the BleManager.
 * <p>
 * Callbacks from {@link McuMgrBleTransport}.
 */
public class McuMgrBleCallbacks implements BleManagerCallbacks {
    public void onCharacteristicRead(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {
    }

    public void onCharacteristicWrite(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {
    }

    public void onDescriptorRead(BluetoothDevice device, BluetoothGattDescriptor descriptor) {
    }

    public void onDescriptorWrite(BluetoothDevice device, BluetoothGattDescriptor descriptor) {
    }

    public void onCharacteristicNotified(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {
    }

    public void onCharacteristicIndicated(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {
    }

    public void onMtuChanged(int mtu) {
    }

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
    public void onLinklossOccur(BluetoothDevice device) {
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
    public void onBonded(BluetoothDevice device) {
    }

    @Override
    public void onError(BluetoothDevice device, String message, int errorCode) {
    }

    @Override
    public void onDeviceNotSupported(BluetoothDevice device) {
    }
}
