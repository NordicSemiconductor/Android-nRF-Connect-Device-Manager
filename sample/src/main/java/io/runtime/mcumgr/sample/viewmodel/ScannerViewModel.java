/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.viewmodel;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.LocationManager;

import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import io.runtime.mcumgr.sample.utils.FilterUtils;
import io.runtime.mcumgr.sample.utils.Utils;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;
import timber.log.Timber;

public class ScannerViewModel extends AndroidViewModel {
    private static final String PREFS_FILTER_UUID_REQUIRED = "filter_uuid";
    private static final String PREFS_FILTER_NEARBY_ONLY = "filter_nearby";

    /** MutableLiveData containing the list of devices. */
    private final DevicesLiveData mDevicesLiveData;
    /** MutableLiveData containing the scanner state. */
    private final ScannerStateLiveData mScannerStateLiveData;

    private final SharedPreferences mPreferences;

    public DevicesLiveData getDevices() {
        return mDevicesLiveData;
    }

    public ScannerStateLiveData getScannerState() {
        return mScannerStateLiveData;
    }

    @Inject
    public ScannerViewModel(@NonNull final Application application,
                            @NonNull final SharedPreferences preferences) {
        super(application);
        mPreferences = preferences;

        final boolean filterUuidRequired = isUuidFilterEnabled();
        final boolean filerNearbyOnly = isNearbyFilterEnabled();

        mScannerStateLiveData = new ScannerStateLiveData(
                Utils.isBleEnabled(),
                Utils.isLocationEnabled(application)
        );
        mDevicesLiveData = new DevicesLiveData(filterUuidRequired, filerNearbyOnly);
        registerBroadcastReceivers(application);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        unregisterBroadcastReceivers(getApplication());
    }

    public boolean isUuidFilterEnabled() {
        return mPreferences.getBoolean(PREFS_FILTER_UUID_REQUIRED, true);
    }

    public boolean isNearbyFilterEnabled() {
        return mPreferences.getBoolean(PREFS_FILTER_NEARBY_ONLY, false);
    }

    /**
     * Forces the observers to be notified. This method is used to refresh the screen after the
     * location permission has been granted. In result, the observer in
     * {@link io.runtime.mcumgr.sample.ScannerActivity} will try to start scanning.
     */
    public void refresh() {
        mScannerStateLiveData.refresh();
    }

    /**
     * Forgets discovered devices.
     */
    public void clear() {
        mDevicesLiveData.clear();
        mScannerStateLiveData.clearRecords();
    }

    /**
     * Updates the device filter. Devices that once passed the filter will still be shown
     * even if they move away from the phone, or change the advertising packet. This is to
     * avoid removing devices from the list.
     *
     * @param uuidRequired if true, the list will display only devices with SMP UUID
     *                     in the advertising packet.
     */
    public void filterByUuid(final boolean uuidRequired) {
        mPreferences.edit().putBoolean(PREFS_FILTER_UUID_REQUIRED, uuidRequired).apply();
        if (mDevicesLiveData.filterByUuid(uuidRequired))
            mScannerStateLiveData.recordFound();
        else
            mScannerStateLiveData.clearRecords();
    }

    /**
     * Updates the device filter. Devices that once passed the filter will still be shown
     * even if they move away from the phone, or change the advertising packet. This is to
     * avoid removing devices from the list.
     *
     * @param nearbyOnly if true, the list will show only devices with high RSSI.
     */
    public void filterByDistance(final boolean nearbyOnly) {
        mPreferences.edit().putBoolean(PREFS_FILTER_NEARBY_ONLY, nearbyOnly).apply();
        if (mDevicesLiveData.filterByDistance(nearbyOnly))
            mScannerStateLiveData.recordFound();
        else
            mScannerStateLiveData.clearRecords();
    }

    /**
     * Start scanning for Bluetooth LE devices.
     */
    public void startScan() {
        if (mScannerStateLiveData.isScanning()) {
            return;
        }

        // Scanning settings
        final ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setLegacy(false)
                .setReportDelay(500)
                .setUseHardwareBatchingIfSupported(false)
                .build();

        final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        scanner.startScan(null, settings, scanCallback);
        mScannerStateLiveData.scanningStarted();
    }

    /**
     * Stop scanning for Bluetooth LE devices.
     */
    public void stopScan() {
        final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        scanner.stopScan(scanCallback);
        mScannerStateLiveData.scanningStopped();
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(final int callbackType, @NonNull final ScanResult result) {
            // This callback will be called only if the scan report delay is not set or is set to 0.

            // If the packet has been obtained while Location was disabled, mark Location as not required
            if (Utils.isLocationRequired(getApplication()) && !Utils.isLocationEnabled(getApplication()))
                Utils.markLocationNotRequired(getApplication());

            if (!isNoise(result) && mDevicesLiveData.deviceDiscovered(result)) {
                mDevicesLiveData.applyFilter();
                mScannerStateLiveData.recordFound();
            }
        }

        @Override
        public void onBatchScanResults(@NonNull final List<ScanResult> results) {
            // This callback will be called only if the report delay set above is greater then 0.
            if (results.isEmpty())
                return;

            // If the packet has been obtained while Location was disabled, mark Location as not required
            if (Utils.isLocationRequired(getApplication()) && !Utils.isLocationEnabled(getApplication()))
                Utils.markLocationNotRequired(getApplication());

            boolean atLeastOneMatchedFilter = false;
            for (final ScanResult result : results)
                atLeastOneMatchedFilter =
                        (!isNoise(result) && mDevicesLiveData.deviceDiscovered(result))
                                || atLeastOneMatchedFilter;
            if (atLeastOneMatchedFilter) {
                mDevicesLiveData.applyFilter();
                mScannerStateLiveData.recordFound();
            }
        }

        @Override
        public void onScanFailed(final int errorCode) {
            Timber.w("Scanning failed with code %d", errorCode);

            if (errorCode == ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED) {
                stopScan();
                startScan();
            }
        }
    };

    /**
     * Register for required broadcast receivers.
     */
    private void registerBroadcastReceivers(@NonNull final Application application) {
        application.registerReceiver(mBluetoothStateBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        if (Utils.isMarshmallowOrAbove()) {
            application.registerReceiver(mLocationProviderChangedReceiver, new IntentFilter(LocationManager.MODE_CHANGED_ACTION));
        }
    }

    /**
     * Unregister all broadcast receivers.
     */
    private void unregisterBroadcastReceivers(@NonNull final Context context) {
        context.unregisterReceiver(mBluetoothStateBroadcastReceiver);

        if (Utils.isMarshmallowOrAbove()) {
            context.unregisterReceiver(mBluetoothStateBroadcastReceiver);
        }
    }

    /**
     * Broadcast receiver to monitor the changes in the location provider
     */
    private final BroadcastReceiver mLocationProviderChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final boolean enabled = Utils.isLocationEnabled(context);
            mScannerStateLiveData.setLocationEnabled(enabled);
        }
    };

    /**
     * Broadcast receiver to monitor the changes in the bluetooth adapter
     */
    private final BroadcastReceiver mBluetoothStateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
            final int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF);

            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    mScannerStateLiveData.bluetoothEnabled();
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                case BluetoothAdapter.STATE_OFF:
                    if (previousState != BluetoothAdapter.STATE_TURNING_OFF && previousState != BluetoothAdapter.STATE_OFF) {
                        stopScan();
                        mDevicesLiveData.bluetoothDisabled();
                        mScannerStateLiveData.bluetoothDisabled();
                    }
                    break;
            }
        }
    };

    /**
     * This method returns true if the scan result may be considered as noise.
     * This is to make the device list on the scanner screen shorter.
     * <p>
     * This implementation considers as noise devices that:
     * <ul>
     * <li>Are not connectable (Android Oreo or newer only),</li>
     * <li>Are far away (RSSI < -80),</li>
     * <li>Advertise as beacons (iBeacons, Nordic Beacons, Microsoft Advertising Beacons,
     * Eddystone),</li>
     * <li>Advertise with AirDrop footprint,</li>
     * </ul>
     * Noise devices will no the shown on the scanner screen even with all filters disabled.
     *
     * @param result the scan result.
     * @return true, if the device may be dismissed, false otherwise.
     */
    @SuppressWarnings({"BooleanMethodIsAlwaysInverted", "RedundantIfStatement"})
    private boolean isNoise(@NonNull final ScanResult result) {
        // Do not show non-connectable devices.
        // Only Android Oreo or newer can say if a device is connectable. On older Android versions
        // the Support Scanner Library assumes all devices are connectable (compatibility mode).
        if (!result.isConnectable())
            return true;

        // Very distant devices are noise.
        if (result.getRssi() < -80)
            return true;

        if (FilterUtils.isBeacon(result))
            return true;

        if (FilterUtils.isAirDrop(result))
            return true;

        return false;
    }
}
