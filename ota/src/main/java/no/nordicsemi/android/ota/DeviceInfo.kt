package no.nordicsemi.android.ota

import com.memfault.cloud.sdk.MemfaultDeviceInfo

/**
 * Information about the device, such as hardware version, software type and current firmware version,
 * required to check for the latest firmware release on nRF Cloud Services.
 *
 * @property deviceSerial The device serial number, also known as Device ID.
 * @property hardwareVersion The hardware version of the device.
 * @property currentVersion The current firmware version running on the device.
 * @property softwareType The software type of the device.
 */
data class DeviceInfo(
    val deviceSerial: String,
    val hardwareVersion: String,
    val currentVersion: String,
    val softwareType: String,
)

/**
 * Maps this [DeviceInfo] to [MemfaultDeviceInfo] required by the Memfault SDK.
 */
internal fun DeviceInfo.map(): MemfaultDeviceInfo = MemfaultDeviceInfo(
    deviceSerial = deviceSerial,
    hardwareVersion = hardwareVersion,
    currentVersion = currentVersion,
    softwareType = softwareType,
)