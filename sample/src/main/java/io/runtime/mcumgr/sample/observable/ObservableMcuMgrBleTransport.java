/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.observable;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.runtime.mcumgr.ble.McuMgrBleTransport;
import no.nordicsemi.android.ble.observer.BondingObserver;

public class ObservableMcuMgrBleTransport extends McuMgrBleTransport {
    private final MutableLiveData<ConnectionState> mConnectionState = new MutableLiveData<>(ConnectionState.DISCONNECTED);
    private final MutableLiveData<BondingState> mBondingState = new MutableLiveData<>(BondingState.NOT_BONDED);

    /**
     * Construct a McuMgrBleTransport object.
     *
     * @param context the context used to connect to the device.
     * @param device  the device to connect to and communicate with.
     * @param handler the handler for BLE calls.
     */
    public ObservableMcuMgrBleTransport(@NonNull final Context context,
                                        @NonNull final BluetoothDevice device,
                                        @NonNull final Handler handler) {
        super(context, device, handler);

        setConnectionObserver(new no.nordicsemi.android.ble.observer.ConnectionObserver() {
            @Override
            public void onDeviceConnecting(@NonNull final BluetoothDevice device) {
                mConnectionState.postValue(ConnectionState.CONNECTING);
            }

            @Override
            public void onDeviceConnected(@NonNull final BluetoothDevice device) {
                mConnectionState.postValue(ConnectionState.INITIALIZING);
            }

            @Override
            public void onDeviceFailedToConnect(@NonNull final BluetoothDevice device, final int reason) {
                if (reason == no.nordicsemi.android.ble.observer.ConnectionObserver.REASON_TIMEOUT) {
                    mConnectionState.postValue(ConnectionState.TIMEOUT);
                } else {
                    mConnectionState.postValue(ConnectionState.DISCONNECTED);
                }
            }

            @Override
            public void onDeviceReady(@NonNull final BluetoothDevice device) {
                mConnectionState.postValue(ConnectionState.READY);
            }

            @Override
            public void onDeviceDisconnecting(@NonNull final BluetoothDevice device) {
                mConnectionState.postValue(ConnectionState.DISCONNECTING);
            }

            @Override
            public void onDeviceDisconnected(@NonNull final BluetoothDevice device, final int reason) {
                if (reason == no.nordicsemi.android.ble.observer.ConnectionObserver.REASON_NOT_SUPPORTED) {
                    mConnectionState.postValue(ConnectionState.NOT_SUPPORTED);
                } else {
                    mConnectionState.postValue(ConnectionState.DISCONNECTED);
                }
            }
        });
        setBondingObserver(new BondingObserver() {
            @Override
            public void onBondingRequired(@NonNull final BluetoothDevice device) {
                mBondingState.postValue(BondingState.BONDING);
            }

            @Override
            public void onBonded(@NonNull final BluetoothDevice device) {
                mBondingState.postValue(BondingState.BONDED);
            }

            @Override
            public void onBondingFailed(@NonNull final BluetoothDevice device) {
                mBondingState.postValue(BondingState.NOT_BONDED);
            }
        });
        setLoggingEnabled(true);
    }

    @Nullable
    public LiveData<ConnectionState> getState() {
        return mConnectionState;
    }

    @Nullable
    public LiveData<BondingState> getBondingState() {
        return mBondingState;
    }
}
