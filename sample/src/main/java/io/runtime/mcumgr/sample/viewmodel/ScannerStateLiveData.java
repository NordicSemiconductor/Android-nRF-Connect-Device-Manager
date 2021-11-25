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
    private boolean mScanningStarted;
    private boolean mHasRecords;
    private boolean mBluetoothEnabled;
    private boolean mLocationEnabled;

    /* package */ ScannerStateLiveData(final boolean bluetoothEnabled, final boolean locationEnabled) {
        mScanningStarted = false;
        mBluetoothEnabled = bluetoothEnabled;
        mLocationEnabled = locationEnabled;
    }

    /* package */ void refresh() {
        postValue(this);
    }

    /* package */ void scanningStarted() {
        mScanningStarted = true;
        postValue(this);
    }

    /* package */ void scanningStopped() {
        mScanningStarted = false;
        postValue(this);
    }

    /* package */ void bluetoothEnabled() {
        mBluetoothEnabled = true;
        postValue(this);
    }

    /* package */
    synchronized void bluetoothDisabled() {
        mBluetoothEnabled = false;
        mHasRecords = false;
        postValue(this);
    }

    /* package */ void setLocationEnabled(final boolean enabled) {
        mLocationEnabled = enabled;
        postValue(this);
    }

    /**
     * Notifies observers that a record has been found.
     */
    /* package */ void recordFound() {
        if (!mHasRecords) {
            mHasRecords = true;
            postValue(this);
        }
    }

    /**
     * Notifies observers that scanner has no records to show.
     */
    /* package */ void clearRecords() {
        if (mHasRecords) {
            mHasRecords = false;
            postValue(this);
        }
    }

    /**
     * Returns whether scanning is in progress.
     */
    public boolean isScanning() {
        return mScanningStarted;
    }

    /**
     * Returns whether any records matching filter criteria has been found.
     */
    public boolean hasRecords() {
        return mHasRecords;
    }

    /**
     * Returns whether Bluetooth adapter is enabled.
     */
    public boolean isBluetoothEnabled() {
        return mBluetoothEnabled;
    }

    /**
     * Returns whether Location is enabled.
     */
    public boolean isLocationEnabled() {
        return mLocationEnabled;
    }
}
