package io.runtime.mcumgr.sample.observable;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import io.runtime.mcumgr.ble.McuMgrBleTransport;
import no.nordicsemi.android.ble.observer.BondingObserver;

public class ObservableMcuMgrBleTransport extends McuMgrBleTransport {
    private MutableLiveData<ConnectionState> mConnectionState = new MutableLiveData<>(ConnectionState.DISCONNECTED);
    private MutableLiveData<BondingState> mBondingState = new MutableLiveData<>(BondingState.NOT_BONDED);

    /**
     * Construct a McuMgrBleTransport object.
     *
     * @param context the context used to connect to the device.
     * @param device  the device to connect to and communicate with.
     */
    public ObservableMcuMgrBleTransport(@NonNull final Context context, @NonNull final BluetoothDevice device) {
        super(context, device);

        setConnectionObserver(new no.nordicsemi.android.ble.observer.ConnectionObserver() {
            @Override
            public void onDeviceConnecting(@NonNull final BluetoothDevice device) {
                mConnectionState.setValue(ConnectionState.CONNECTING);
            }

            @Override
            public void onDeviceConnected(@NonNull final BluetoothDevice device) {
                mConnectionState.setValue(ConnectionState.INITIALIZING);
            }

            @Override
            public void onDeviceFailedToConnect(@NonNull final BluetoothDevice device, final int reason) {
                if (reason == no.nordicsemi.android.ble.observer.ConnectionObserver.REASON_TIMEOUT) {
                    mConnectionState.setValue(ConnectionState.TIMEOUT);
                } else {
                    mConnectionState.setValue(ConnectionState.DISCONNECTED);
                }
            }

            @Override
            public void onDeviceReady(@NonNull final BluetoothDevice device) {
                mConnectionState.setValue(ConnectionState.READY);
            }

            @Override
            public void onDeviceDisconnecting(@NonNull final BluetoothDevice device) {
                mConnectionState.setValue(ConnectionState.DISCONNECTING);
            }

            @Override
            public void onDeviceDisconnected(@NonNull final BluetoothDevice device, final int reason) {
                if (reason == no.nordicsemi.android.ble.observer.ConnectionObserver.REASON_NOT_SUPPORTED) {
                    mConnectionState.setValue(ConnectionState.NOT_SUPPORTED);
                } else {
                    mConnectionState.setValue(ConnectionState.DISCONNECTED);
                }
            }
        });
        setBondingObserver(new BondingObserver() {
            @Override
            public void onBondingRequired(@NonNull final BluetoothDevice device) {
                mBondingState.setValue(BondingState.BONDING);
            }

            @Override
            public void onBonded(@NonNull final BluetoothDevice device) {
                mBondingState.setValue(BondingState.BONDED);
            }

            @Override
            public void onBondingFailed(@NonNull final BluetoothDevice device) {
                mBondingState.setValue(BondingState.NOT_BONDED);
            }
        });
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
