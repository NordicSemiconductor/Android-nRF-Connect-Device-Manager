@file:Suppress("unused")

package no.nordicsemi.memfault.observability.bluetooth

import no.nordicsemi.kotlin.ble.core.ConnectionState
import no.nordicsemi.memfault.observability.bluetooth.DeviceState.Disconnected.Reason
import no.nordicsemi.memfault.observability.data.DeviceConfig

/**
 * Represents the state of the device Bluetooth LE connection.
 */
sealed class DeviceState {
	/** The device is currently connecting. */
	data object Connecting : DeviceState()
	/** The device is being initialized. */
	data object Initializing : DeviceState()
	/**
	 * The device is connected and set up to receive Diagnostic data.
	 *
	 * @property config The configuration obtained from the device using GATT.
	 */
	data class Connected(val config: DeviceConfig) : DeviceState()
	/** The device is currently disconnecting. */
	data object Disconnecting : DeviceState()
	/** The device is disconnected. */
	data class Disconnected(val reason: Reason? = null) : DeviceState() {

		/**
		 * Represents the reason for disconnection or failure to connect.
		 */
		enum class Reason {
			FAILED_TO_CONNECT,
			NOT_SUPPORTED,
			BONDING_FAILED,
			CONNECTION_LOST,
			TIMEOUT,
		}

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (other !is Disconnected) return false
			return true
		}

        override fun hashCode(): Int {
            return reason?.hashCode() ?: 0
        }
    }
}

internal fun ConnectionState.toDeviceState(
	notSupported: Boolean = false,
	bondingFailed: Boolean = false,
): DeviceState = when (this) {
	ConnectionState.Connecting,
	ConnectionState.Connected -> DeviceState.Connecting // <- State DeviceState.Connected is emitted manually after initialization.
	ConnectionState.Disconnecting -> DeviceState.Disconnecting
	is ConnectionState.Disconnected ->
		when (reason) {
			null,
			ConnectionState.Disconnected.Reason.Success,
            ConnectionState.Disconnected.Reason.Cancelled ->
				DeviceState.Disconnected(
					reason = when {
						bondingFailed -> Reason.BONDING_FAILED
						notSupported -> Reason.NOT_SUPPORTED
						else -> null
					}
				)
			is ConnectionState.Disconnected.Reason.Timeout ->
				DeviceState.Disconnected(Reason.TIMEOUT)
			ConnectionState.Disconnected.Reason.TerminateLocalHost -> if (bondingFailed)
				DeviceState.Disconnected(Reason.BONDING_FAILED)
				else DeviceState.Disconnected(Reason.CONNECTION_LOST)
			ConnectionState.Disconnected.Reason.TerminatePeerUser,
			ConnectionState.Disconnected.Reason.LinkLoss ->
				DeviceState.Disconnected(Reason.CONNECTION_LOST)
			ConnectionState.Disconnected.Reason.UnsupportedAddress,
			ConnectionState.Disconnected.Reason.InsufficientAuthentication ->
				DeviceState.Disconnected(Reason.BONDING_FAILED)
			is ConnectionState.Disconnected.Reason.Unknown,
			ConnectionState.Disconnected.Reason.UnsupportedConfiguration ->
				DeviceState.Disconnected(Reason.FAILED_TO_CONNECT)
		}
}