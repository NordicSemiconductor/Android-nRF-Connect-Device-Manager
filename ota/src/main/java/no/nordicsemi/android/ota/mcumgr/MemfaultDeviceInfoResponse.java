/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.ota.mcumgr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nordicsemi.android.mcumgr.response.McuMgrResponse;
import no.nordicsemi.android.ota.DeviceInfo;

public class MemfaultDeviceInfoResponse extends McuMgrResponse implements MemfaultManager.Response {
    /** The device Serial Number as hexadecimal String. */
    @JsonProperty("device_serial")
    public String deviceSerial;

    /** The device Hardware Version, i.e. "nRF54L15dk". */
    @JsonProperty("hardware_version")
    public String hardwareVersion;

    /** The device Software Type, i.e. "app". */
    @JsonProperty("software_type")
    public String softwareType;

    /** The current firmware version, i.e. "1.0.0+ab1234" or "2.1.0". */
    @JsonProperty("current_version")
    public String currentVersion;

    @JsonCreator
    public MemfaultDeviceInfoResponse() {}

    public DeviceInfo toDeviceInfo() {
        return new DeviceInfo(deviceSerial, hardwareVersion, currentVersion, softwareType);
    }
}
