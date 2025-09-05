/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.viewmodel.scanner;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import no.nordicsemi.android.mcumgr.sample.adapter.DiscoveredBluetoothDevice;
import no.nordicsemi.android.support.v18.scanner.ScanResult;

/**
 * This class keeps the current list of discovered Bluetooth LE devices matching filter.
 * Each time @{link {@link #applyFilter()} is called, the observers are notified with a new
 * list instance.
 */
@SuppressWarnings("unused")
public class DevicesLiveData extends LiveData<List<DiscoveredBluetoothDevice>> {
    private static final int FILTER_RSSI = -50; // [dBm]

    private final List<DiscoveredBluetoothDevice> devices = new ArrayList<>();
    private List<DiscoveredBluetoothDevice> filteredDevices = null;
    private boolean filterNamedOnly;
    private boolean filterNearbyOnly;

    /* package */ DevicesLiveData() {
        this.filterNamedOnly = false;
        this.filterNearbyOnly = false;
    }

    /* package */ DevicesLiveData(final boolean filterNamedOnly, final boolean filterNearbyOnly) {
        this.filterNamedOnly = filterNamedOnly;
        this.filterNearbyOnly = filterNearbyOnly;
    }

    /* package */
    synchronized void bluetoothDisabled() {
        devices.clear();
        filteredDevices = null;
        postValue(null);
    }

    /* package */  boolean filterByName(final boolean nameRequired) {
        filterNamedOnly = nameRequired;
        return applyFilter();
    }

    /* package */  boolean filterByDistance(final boolean nearbyOnly) {
        filterNearbyOnly = nearbyOnly;
        return applyFilter();
    }

    /* package */
    synchronized boolean deviceDiscovered(final ScanResult result) {
        DiscoveredBluetoothDevice device;

        // Check if it's a new device.
        final int index = indexOf(result);
        if (index == -1) {
            device = new DiscoveredBluetoothDevice(result);
            devices.add(device);
        } else {
            device = devices.get(index);
        }

        // Update RSSI and name.
        device.update(result);

        // Return true if the device was on the filtered list or is to be added.
        return (filteredDevices != null && filteredDevices.contains(device))
                || (matchesNameFilter(device) && matchesNearbyFilter(device));
    }

    synchronized void setDevices(final Set<BluetoothDevice> devices) {
        this.devices.clear();
        // map devices to list of DiscoveredBluetoothDevice
        for (final BluetoothDevice device : devices) {
            this.devices.add(new DiscoveredBluetoothDevice(device));
        }
        applyFilter();
    }

    /**
     * Clears the list of devices.
     */
    /* package */ synchronized void clear() {
        devices.clear();
        filteredDevices = null;
        postValue(null);
    }

    /**
     * Refreshes the filtered device list based on the filter flags.
     */
    /* package */
    synchronized boolean applyFilter() {
        final List<DiscoveredBluetoothDevice> devices = new ArrayList<>();
        for (final DiscoveredBluetoothDevice device : this.devices) {
            if (matchesNameFilter(device) && matchesNearbyFilter(device)) {
                devices.add(device);
            }
        }
        filteredDevices = devices;
        postValue(filteredDevices);
        return !filteredDevices.isEmpty();
    }

    /**
     * Finds the index of existing devices on the device list.
     *
     * @param result scan result.
     * @return Index of -1 if not found.
     */
    private int indexOf(final ScanResult result) {
        int i = 0;
        for (final DiscoveredBluetoothDevice device : devices) {
            if (device.matches(result))
                return i;
            i++;
        }
        return -1;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private boolean matchesNameFilter(@NonNull final DiscoveredBluetoothDevice device) {
        if (!filterNamedOnly)
            return true;

        return device.hadName();
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private boolean matchesNearbyFilter(@NonNull final DiscoveredBluetoothDevice device) {
        if (!filterNearbyOnly)
            return true;

        return device.getHighestRssi() >= FILTER_RSSI;
    }
}
