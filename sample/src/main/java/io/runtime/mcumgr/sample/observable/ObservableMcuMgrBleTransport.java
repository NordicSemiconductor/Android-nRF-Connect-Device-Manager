/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.observable;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.runtime.mcumgr.ble.McuMgrBleTransport;
import no.nordicsemi.android.ble.observer.BondingObserver;

public class ObservableMcuMgrBleTransport extends McuMgrBleTransport {
    private final MutableLiveData<ConnectionState> connectionState;
    private final MutableLiveData<BondingState> bondingState;

    @Nullable
    private OnReleaseCallback onReleaseCallback;

    public interface OnReleaseCallback {
        void onReleased();
    }

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

        connectionState = new MutableLiveData<>(ConnectionState.of(context, device));
        setConnectionObserver(new no.nordicsemi.android.ble.observer.ConnectionObserver() {
            @Override
            public void onDeviceConnecting(@NonNull final BluetoothDevice device) {
                connectionState.postValue(ConnectionState.CONNECTING);
            }

            @Override
            public void onDeviceConnected(@NonNull final BluetoothDevice device) {
                connectionState.postValue(ConnectionState.INITIALIZING);
            }

            @Override
            public void onDeviceFailedToConnect(@NonNull final BluetoothDevice device, final int reason) {
                if (reason == no.nordicsemi.android.ble.observer.ConnectionObserver.REASON_TIMEOUT) {
                    connectionState.postValue(ConnectionState.TIMEOUT);
                } else {
                    connectionState.postValue(ConnectionState.DISCONNECTED);
                }
            }

            @Override
            public void onDeviceReady(@NonNull final BluetoothDevice device) {
                connectionState.postValue(ConnectionState.READY);
            }

            @Override
            public void onDeviceDisconnecting(@NonNull final BluetoothDevice device) {
                connectionState.postValue(ConnectionState.DISCONNECTING);
            }

            @Override
            public void onDeviceDisconnected(@NonNull final BluetoothDevice device, final int reason) {
                if (reason == no.nordicsemi.android.ble.observer.ConnectionObserver.REASON_NOT_SUPPORTED) {
                    connectionState.postValue(ConnectionState.NOT_SUPPORTED);
                } else {
                    connectionState.postValue(ConnectionState.DISCONNECTED);
                }
            }
        });

        bondingState = new MutableLiveData<>(BondingState.of(device));
        setBondingObserver(new BondingObserver() {
            @Override
            public void onBondingRequired(@NonNull final BluetoothDevice device) {
                bondingState.postValue(BondingState.BONDING);
            }

            @Override
            public void onBonded(@NonNull final BluetoothDevice device) {
                bondingState.postValue(BondingState.BONDED);
            }

            @Override
            public void onBondingFailed(@NonNull final BluetoothDevice device) {
                bondingState.postValue(BondingState.NOT_BONDED);
            }
        });
        setLoggingEnabled(true);
    }

    @Nullable
    public LiveData<ConnectionState> getState() {
        return connectionState;
    }

    @Nullable
    public LiveData<BondingState> getBondingState() {
        return bondingState;
    }

    public void setOnReleasedCallback(@Nullable final OnReleaseCallback callback) {
        onReleaseCallback = callback;
    }

    @Override
    public void release() {
        cancelQueue();
        disconnect()
                // Handling a case when user releases the transport object without connecting to the device.
                // In that case, the BluetoothDevice is not set and DisconnectRequest returns invalid state.
                .invalid(this::close)
                .enqueue();
    }

    @Override
    public void close() {
        super.close();

        if (onReleaseCallback != null) {
            onReleaseCallback.onReleased();
        }
    }
}
