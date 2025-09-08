/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.observable;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import no.nordicsemi.android.ble.annotation.PhyValue;
import no.nordicsemi.android.ble.callback.PhyCallback;
import no.nordicsemi.android.ble.observer.BondingObserver;
import no.nordicsemi.android.mcumgr.ble.McuMgrBleTransport;
import no.nordicsemi.android.ota.DeviceInfo;


public class ObservableMcuMgrBleTransport extends McuMgrBleTransport {
    // For now, the parameters require by nRF Cloud OTA are accessible
    // using Device Information Service (DIS) and Monitoring and Diagnostics Service (MDS).
    private final UUID DIS_SERVICE_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    private final UUID DIS_SERIAL_NUMBER_UUID = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb");
    private final UUID DIS_FW_REV_UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    private final UUID DIS_HW_REV_UUID = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb");
    private final UUID DIS_SW_REV_UUID = UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb");

    private final UUID MDS_SERVICE_UUID = UUID.fromString("54220000-f6a5-4007-a371-722f4ebd8436");
    private final UUID MDS_AUTH_CHAR_UUID = UUID.fromString("54220004-f6a5-4007-a371-722f4ebd8436");

    private final MutableLiveData<ConnectionState> connectionState;
    private final MutableLiveData<BondingState> bondingState;
    private final MutableLiveData<ConnectionParameters> connectionParameters;

    @Nullable
    private OnReleaseCallback onReleaseCallback;

    @Nullable
    private BluetoothGattCharacteristic disSerialNumberCharacteristic, disFwRevCharacteristic,
            disHwRevCharacteristic, disSwRevCharacteristic;
    @Nullable
    private BluetoothGattCharacteristic mdsAuthCharacteristic;
    @Nullable
    private DeviceInfo deviceInfo;
    @Nullable
    private String projectKey;

    @PhyValue
    private int txPhy = PhyCallback.PHY_LE_1M, rxPhy = PhyCallback.PHY_LE_1M;

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            connectionParameters = new MutableLiveData<>(null);
            setConnectionParametersListener((d, interval, latency, timeout) -> {
                final ConnectionParameters parameters = new ConnectionParameters(
                        interval, latency, timeout,
                        getMtu(), getMaxPacketLength(),
                        txPhy, rxPhy);
                connectionParameters.postValue(parameters);
            });
        } else {
            connectionParameters = null;
        }
        setLoggingEnabled(true);
    }

    @Override
    protected boolean isAdditionalServiceSupported(@NonNull BluetoothGatt gatt) {
        final BluetoothGattService disService = gatt.getService(DIS_SERVICE_UUID);
        if (disService != null) {
            disSerialNumberCharacteristic = disService.getCharacteristic(DIS_SERIAL_NUMBER_UUID);
            disFwRevCharacteristic = disService.getCharacteristic(DIS_FW_REV_UUID);
            disHwRevCharacteristic = disService.getCharacteristic(DIS_HW_REV_UUID);
            disSwRevCharacteristic = disService.getCharacteristic(DIS_SW_REV_UUID);
        }
        final BluetoothGattService mdsService = gatt.getService(MDS_SERVICE_UUID);
        if (mdsService != null) {
            mdsAuthCharacteristic = mdsService.getCharacteristic(MDS_AUTH_CHAR_UUID);
        }
        // DIS and MDS are optional, so always return true.
        return true;
    }

    @Override
    protected void initializeAdditionalServices() {
        readPhy()
            .with((device, txPhy, rxPhy) -> {
                this.txPhy = txPhy;
                this.rxPhy = rxPhy;
            })
            .enqueue();

        // For the purpose of nRF Cloud OTA read the device properties:
        // - Serial number
        // - Hardware version
        // - Software type
        // - Current software version
        // - Memfault Project key
        // If Device Information Service or Monitoring and Diagnostics Service
        // or any of the required characteristics are not present, skip reading the values.
        //
        // Note: This is a temporary solution until we have a proper protocol, i.e. SMP.
        if (mdsAuthCharacteristic != null &&
            disSerialNumberCharacteristic != null &&
            disFwRevCharacteristic != null &&
            disHwRevCharacteristic != null &&
            disSwRevCharacteristic != null) {
            AtomicReference<String> serialNumber = new AtomicReference<>();
            AtomicReference<String> hwVersion = new AtomicReference<>();
            AtomicReference<String> swType = new AtomicReference<>();
            AtomicReference<String> currentVersion = new AtomicReference<>();
            beginAtomicRequestQueue()
                    .add(readCharacteristic(disSerialNumberCharacteristic)
                            .with((device, data) -> serialNumber.set(data.getStringValue(0))))
                    .add(readCharacteristic(disHwRevCharacteristic)
                            .with((device, data) -> hwVersion.set(data.getStringValue(0))))
                    .add(readCharacteristic(disFwRevCharacteristic)
                            .with((device, data) -> currentVersion.set(data.getStringValue(0))))
                    .add(readCharacteristic(disSwRevCharacteristic)
                            .with((device, data) -> swType.set(data.getStringValue(0))))
                    .add(readCharacteristic(mdsAuthCharacteristic)
                            .with((device, data) -> {
                                final String value = data.getStringValue(0);
                                if (value != null && !value.isEmpty()) {
                                    // Received value is in format: "Memfault-Project-Key:<project_key>"
                                    final String[] parts = value.split(":");
                                    if (parts.length == 2) {
                                        projectKey = parts[1];
                                    }
                                }
                            }))
                    .done(device -> deviceInfo = new DeviceInfo(
                            serialNumber.get(),
                            hwVersion.get(),
                            currentVersion.get(),
                            swType.get()
                    ))
                    .then(device -> {
                        if (deviceInfo != null && projectKey != null) {
                            log(Log.INFO, "nRF Cloud OTA Supported: " + deviceInfo + ", Project Key: " + projectKey);
                        }
                    })
                    .enqueue();

        }
    }

    @NonNull
    public LiveData<ConnectionState> getState() {
        return connectionState;
    }

    @NonNull
    public LiveData<BondingState> getBondingState() {
        return bondingState;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @NonNull
    public MutableLiveData<ConnectionParameters> getConnectionParameters() {
        return connectionParameters;
    }

    @Nullable
    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    @Nullable
    public String getProjectKey() {
        return projectKey;
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
                .then(device -> close())
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
