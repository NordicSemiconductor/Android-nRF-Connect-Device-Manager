/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.viewmodel.scanner;

import android.Manifest;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.AndroidViewModel;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import no.nordicsemi.android.mcumgr.sample.fragment.scanner.SavedDevicesFragment;
import no.nordicsemi.android.mcumgr.sample.utils.Utils;

public class SavedDevicesViewModel extends AndroidViewModel {
    /** MutableLiveData containing the list of devices. */
    private final DevicesLiveData devicesLiveData;
    /** MutableLiveData containing the scanner state. */
    private final ScannerStateLiveData scannerStateLiveData;

    public DevicesLiveData getDevices() {
        return devicesLiveData;
    }

    public ScannerStateLiveData getScannerState() {
        return scannerStateLiveData;
    }

    @Inject
    public SavedDevicesViewModel(@NonNull final Application application) {
        super(application);

        scannerStateLiveData = new ScannerStateLiveData(
                Utils.isBleEnabled(),
                Utils.isLocationEnabled(application)
        );
        devicesLiveData = new DevicesLiveData();
        registerBroadcastReceivers(application);

        refresh();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        unregisterBroadcastReceivers(getApplication());
    }

    /**
     * Forces the observers to be notified. This method is used to refresh the screen after the
     * location permission has been granted. In result, the observer in
     * {@link SavedDevicesFragment} will try to start scanning.
     */
    public void refresh() {
        scannerStateLiveData.refresh();
    }

    public void showDevices() {
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (ActivityCompat.checkSelfPermission(getApplication(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        final Set<BluetoothDevice> devices = new HashSet<>();
        for (BluetoothDevice device : adapter.getBondedDevices()) {
            if (device.getType() != BluetoothDevice.DEVICE_TYPE_CLASSIC) {
                devices.add(device);
            }
        }
        devicesLiveData.setDevices(devices);
        if (!devices.isEmpty()) {
            scannerStateLiveData.recordFound();
        }
    }

    /**
     * Forgets discovered devices.
     */
    public void clear() {
        devicesLiveData.clear();
        scannerStateLiveData.clearRecords();
    }

    /**
     * Register for required broadcast receivers.
     */
    private void registerBroadcastReceivers(@NonNull final Application application) {
        application.registerReceiver(bluetoothStateBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    /**
     * Unregister all broadcast receivers.
     */
    private void unregisterBroadcastReceivers(@NonNull final Context context) {
        context.unregisterReceiver(bluetoothStateBroadcastReceiver);
    }

    /**
     * Broadcast receiver to monitor the changes in the bluetooth adapter
     */
    private final BroadcastReceiver bluetoothStateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
            final int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF);

            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    scannerStateLiveData.bluetoothEnabled();
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                case BluetoothAdapter.STATE_OFF:
                    if (previousState != BluetoothAdapter.STATE_TURNING_OFF && previousState != BluetoothAdapter.STATE_OFF) {
                        devicesLiveData.bluetoothDisabled();
                        scannerStateLiveData.bluetoothDisabled();
                    }
                    break;
            }
        }
    };
}
