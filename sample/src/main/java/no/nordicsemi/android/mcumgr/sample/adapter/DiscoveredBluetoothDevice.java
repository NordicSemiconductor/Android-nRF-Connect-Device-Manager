/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package no.nordicsemi.android.mcumgr.sample.adapter;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class DiscoveredBluetoothDevice implements Parcelable {
    private final BluetoothDevice device;
    private ScanResult lastScanResult;
    private String name;
    private int rssi;
    private int previousRssi;
    private int highestRssi = -128;
    /**
     * A flag indicating that the device advertised Local Name at least once.
     * <p>
     * Some devices advertise with multiple different advertising packets, some with and some
     * without names. To avoid flickering with "Only named" filter enabled this will show
     * devices that have or HAD local name in their advertising packet.
     */
    private boolean hadName;

    public DiscoveredBluetoothDevice(final ScanResult scanResult) {
        device = scanResult.getDevice();
        update(scanResult);
    }

    public DiscoveredBluetoothDevice(final BluetoothDevice device) {
        this.device = device;
        try {
            this.name = device.getName();
        } catch (final SecurityException e) {
            this.name = null;
        }
        this.lastScanResult = null;
        this.rssi = -128;
        this.highestRssi = -128;
        this.previousRssi = -128;
        this.hadName = this.name != null && !this.name.isEmpty();
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public String getAddress() {
        return device.getAddress();
    }

    public String getName() {
        return name;
    }

    /**
     * A flag indicating that the device advertised Local Name at least once.
     * <p>
     * Some devices advertise with multiple different advertising packets, some with and some
     * without names. To avoid flickering with "Only named" filter enabled this will show
     * devices that have or HAD local name in their advertising packet.
     */
    public boolean hadName() {
        return hadName;
    }

    public int getRssi() {
        return rssi;
    }

    /**
     * Returns the highest recorded RSSI value during the scan.
     *
     * @return Highest RSSI value.
     */
    public int getHighestRssi() {
        return highestRssi;
    }

    /**
     * This method returns true if the RSSI range has changed. The RSSI range depends on drawable
     * levels from {@link no.nordicsemi.android.mcumgr.sample.R.drawable#ic_rssi_bar}.
     *
     * @return true, if the RSSI range has changed.
     */
    /* package */ boolean hasRssiLevelChanged() {
        final int newLevel =
                rssi <= 10 ?
                        0 :
                        rssi <= 28 ?
                                1 :
                                rssi <= 45 ?
                                        2 :
                                        3;
        final int oldLevel =
                previousRssi <= 10 ?
                        0 :
                        previousRssi <= 28 ?
                                1 :
                                previousRssi <= 45 ?
                                        2 :
                                        3;
        return newLevel != oldLevel;
    }

    /**
     * Updates the device values based on the scan result.
     *
     * @param scanResult the new received scan result.
     */
    public void update(final ScanResult scanResult) {
        lastScanResult = scanResult;
        name = scanResult.getScanRecord() != null ?
                scanResult.getScanRecord().getDeviceName() : null;
        if (!hadName)
            hadName = name != null && !name.isEmpty();
        previousRssi = rssi;
        rssi = scanResult.getRssi();
        if (highestRssi < rssi)
            highestRssi = rssi;
    }

    public boolean matches(final ScanResult scanResult) {
        return device.getAddress().equals(scanResult.getDevice().getAddress());
    }

    @Override
    public int hashCode() {
        return device.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof DiscoveredBluetoothDevice that) {
            return device.getAddress().equals(that.device.getAddress());
        }
        return super.equals(o);
    }

    // Parcelable implementation

    private DiscoveredBluetoothDevice(final Parcel in) {
        device = in.readParcelable(BluetoothDevice.class.getClassLoader());
        lastScanResult = in.readParcelable(ScanResult.class.getClassLoader());
        name = in.readString();
        rssi = in.readInt();
        previousRssi = in.readInt();
        highestRssi = in.readInt();
        hadName = in.readInt() == 1;
    }

    @Override
    public void writeToParcel(final Parcel parcel, final int flags) {
        parcel.writeParcelable(device, flags);
        parcel.writeParcelable(lastScanResult, flags);
        parcel.writeString(name);
        parcel.writeInt(rssi);
        parcel.writeInt(previousRssi);
        parcel.writeInt(highestRssi);
        parcel.writeInt(hadName ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DiscoveredBluetoothDevice> CREATOR = new Creator<>() {
        @Override
        public DiscoveredBluetoothDevice createFromParcel(final Parcel source) {
            return new DiscoveredBluetoothDevice(source);
        }

        @Override
        public DiscoveredBluetoothDevice[] newArray(final int size) {
            return new DiscoveredBluetoothDevice[size];
        }
    };
}
