/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.viewmodel;

import androidx.lifecycle.LiveData;

/**
 * This class keeps the current state of the scanner.
 */
@SuppressWarnings("unused")
public class ScannerStateLiveData extends LiveData<ScannerStateLiveData> {
    private boolean scanningStarted;
    private boolean hasRecords;
    private boolean bluetoothEnabled;
    private boolean locationEnabled;

    /* package */ ScannerStateLiveData(final boolean bluetoothEnabled, final boolean locationEnabled) {
        this.scanningStarted = false;
        this.bluetoothEnabled = bluetoothEnabled;
        this.locationEnabled = locationEnabled;
    }

    /* package */ void refresh() {
        postValue(this);
    }

    /* package */ void scanningStarted() {
        scanningStarted = true;
        postValue(this);
    }

    /* package */ void scanningStopped() {
        scanningStarted = false;
        postValue(this);
    }

    /* package */ void bluetoothEnabled() {
        bluetoothEnabled = true;
        postValue(this);
    }

    /* package */
    synchronized void bluetoothDisabled() {
        bluetoothEnabled = false;
        hasRecords = false;
        postValue(this);
    }

    /* package */ void setLocationEnabled(final boolean enabled) {
        locationEnabled = enabled;
        postValue(this);
    }

    /**
     * Notifies observers that a record has been found.
     */
    /* package */ void recordFound() {
        if (!hasRecords) {
            hasRecords = true;
            postValue(this);
        }
    }

    /**
     * Notifies observers that scanner has no records to show.
     */
    /* package */ void clearRecords() {
        if (hasRecords) {
            hasRecords = false;
            postValue(this);
        }
    }

    /**
     * Returns whether scanning is in progress.
     */
    public boolean isScanning() {
        return scanningStarted;
    }

    /**
     * Returns whether any records matching filter criteria has been found.
     */
    public boolean hasRecords() {
        return hasRecords;
    }

    /**
     * Returns whether Bluetooth adapter is enabled.
     */
    public boolean isBluetoothEnabled() {
        return bluetoothEnabled;
    }

    /**
     * Returns whether Location is enabled.
     */
    public boolean isLocationEnabled() {
        return locationEnabled;
    }
}
