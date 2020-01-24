/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.viewmodel.mcumgr;

import android.bluetooth.BluetoothDevice;

import javax.inject.Inject;
import javax.inject.Named;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.ble.McuMgrBleCallbacksStub;
import io.runtime.mcumgr.ble.McuMgrBleTransport;
import io.runtime.mcumgr.sample.R;

public class DeviceStatusViewModel extends McuMgrViewModel {
    private final MutableLiveData<Integer> mConnectionStateLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> mBondStateLiveData = new MutableLiveData<>();

    @Inject
    DeviceStatusViewModel(final McuMgrTransport transport,
                          @Named("busy") final MutableLiveData<Boolean> state) {
        super(state);
        if (transport instanceof McuMgrBleTransport) {
            ((McuMgrBleTransport) transport).setGattCallbacks(new DeviceCallbacks());
        }
    }

    public LiveData<Integer> getConnectionState() {
        return mConnectionStateLiveData;
    }

    public LiveData<Integer> getBondState() {
        return mBondStateLiveData;
    }

    private final class DeviceCallbacks extends McuMgrBleCallbacksStub {
        @Override
        public void onDeviceConnecting(@NonNull final BluetoothDevice device) {
            mConnectionStateLiveData.postValue(R.string.status_connecting);
        }

        @Override
        public void onDeviceConnected(@NonNull final BluetoothDevice device) {
            mConnectionStateLiveData.postValue(R.string.status_initializing);
        }

        @Override
        public void onDeviceReady(@NonNull final BluetoothDevice device) {
            mConnectionStateLiveData.postValue(R.string.status_connected);
        }

        @Override
        public void onDeviceDisconnecting(@NonNull final BluetoothDevice device) {
            mConnectionStateLiveData.postValue(R.string.status_disconnecting);
        }

        @Override
        public void onDeviceDisconnected(@NonNull final BluetoothDevice device) {
            mConnectionStateLiveData.postValue(R.string.status_disconnected);
        }

        @Override
        public void onBondingRequired(@NonNull final BluetoothDevice device) {
            mBondStateLiveData.postValue(R.string.status_bonding);
        }

        @Override
        public void onBonded(@NonNull final BluetoothDevice device) {
            mBondStateLiveData.postValue(R.string.status_bonded);
        }

        @Override
        public void onBondingFailed(@NonNull final BluetoothDevice device) {
            mBondStateLiveData.postValue(R.string.status_not_bonded);
        }
    }
}
