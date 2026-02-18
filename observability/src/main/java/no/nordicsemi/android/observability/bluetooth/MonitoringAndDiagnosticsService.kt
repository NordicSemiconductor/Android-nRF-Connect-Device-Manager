/*
 * Copyright (c) 2022, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list
 * of conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

@file:OptIn(ExperimentalUuidApi::class)
@file:Suppress("unused")

package no.nordicsemi.android.observability.bluetooth

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import no.nordicsemi.android.observability.data.ChunksConfig
import no.nordicsemi.android.observability.internal.AuthorisationHeader
import no.nordicsemi.android.observability.internal.map
import no.nordicsemi.kotlin.ble.client.RemoteService
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.Peripheral
import no.nordicsemi.kotlin.ble.client.android.native
import no.nordicsemi.kotlin.ble.client.exception.OperationFailedException
import no.nordicsemi.kotlin.ble.core.BondState
import no.nordicsemi.kotlin.ble.core.CharacteristicProperty
import no.nordicsemi.kotlin.ble.core.ConnectionState
import no.nordicsemi.kotlin.ble.core.Manager
import no.nordicsemi.kotlin.ble.core.OperationStatus
import no.nordicsemi.kotlin.ble.core.WriteType
import no.nordicsemi.kotlin.ble.environment.android.NativeAndroidEnvironment
import org.slf4j.LoggerFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Monitoring & Diagnostics Service UUID.
 *
 * Find specification ih the [Documentation](https://docs.memfault.com/docs/mcu/mds).
 */
val MDS_SERVICE_UUID = Uuid.parse("54220000-f6a5-4007-a371-722f4ebd8436")

private val MDS_SUPPORTED_FEATURES_CHARACTERISTIC_UUID = Uuid.parse("54220001-f6a5-4007-a371-722f4ebd8436")
private val MDS_DEVICE_ID_CHARACTERISTIC_UUID          = Uuid.parse("54220002-f6a5-4007-a371-722f4ebd8436")
private val MDS_DATA_URI_CHARACTERISTIC_UUID           = Uuid.parse("54220003-f6a5-4007-a371-722f4ebd8436")
private val MDS_AUTHORISATION_CHARACTERISTIC_UUID      = Uuid.parse("54220004-f6a5-4007-a371-722f4ebd8436")
private val MDS_DATA_EXPORT_CHARACTERISTIC_UUID        = Uuid.parse("54220005-f6a5-4007-a371-722f4ebd8436")

/**
 * A client implementation of Monitoring & Diagnostics Service (MDS) that streams data
 * from the device.
 *
 * This class connects to the device, discovers the MDS service, reads the configuration,
 * and streams diagnostics [chunks] from the device.
 */
class MonitoringAndDiagnosticsService {
	private val logger = LoggerFactory.getLogger(MonitoringAndDiagnosticsService::class.java)

	/**
	 * Represents the state of the device Bluetooth LE connection.
	 */
	sealed class State {
		/** The device is currently connecting. */
		data object Connecting : State()
		/** The device is being initialized. */
		data object Initializing : State()
		/**
		 * The device is connected and set up to receive Diagnostic data.
		 *
		 * @property config The configuration obtained from the device using GATT.
		 */
		data class Connected(val config: ChunksConfig) : State()
		/** The device is currently disconnecting. */
		data object Disconnecting : State()
		/** The device is disconnected. */
		data class Disconnected(val reason: Reason? = null) : State() {

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

	/**
	 * Creates a new instance of [MonitoringAndDiagnosticsService] with the given
	 * [CentralManager] and [Peripheral].
	 *
	 * This constructor can be user with a 'native' or 'mock' [CentralManager].
	 *
	 * @param centralManager The central manager to use for connection.
	 * @param peripheral The peripheral to connect to.
	 * @param scope The coroutine scope.
	 */
	constructor(
		centralManager: CentralManager,
		peripheral: Peripheral,
		scope: CoroutineScope,
	) {
		this.scope = scope
		this.centralManager = centralManager
		this.peripheral = peripheral
	}

	/**
	 * Creates a new instance of [MonitoringAndDiagnosticsService] with the given
	 * [NativeAndroidEnvironment] and [BluetoothDevice].
	 *
	 * This constructor is for legacy applications that use the Android Bluetooth API.
	 *
	 * @param environment The native Android Environment object.
	 * @param bluetoothDevice The Bluetooth device to connect to.
	 * @param scope The coroutine scope.
	 */
	constructor(
		environment: NativeAndroidEnvironment,
		bluetoothDevice: BluetoothDevice,
		scope: CoroutineScope
	) {
		this.scope = scope
		this.centralManager = CentralManager.native(environment, scope)
		this.peripheral = centralManager.getPeripheralById(bluetoothDevice.address)!!
	}

	/** The coroutine scope used for launching flows and coroutines. */
	private val scope: CoroutineScope
	private var job: Job? = null
	/** The central manager used to connect to the device. */
	private val centralManager: CentralManager
	/** The peripheral representing the device to connect to. */
	private val peripheral: Peripheral
	/** A flag set when no MDS service was found. */
	private var notSupported = false
	/** A flag set when the bonding process failed. */
	private var bondingFailed = false

	private val _state = MutableStateFlow<State>(State.Disconnected())
	private val _chunks = MutableSharedFlow<ByteArray>(
		extraBufferCapacity = 25,
		onBufferOverflow = BufferOverflow.SUSPEND
	)

	/** The current state of the device. */
	val state = _state.asStateFlow()
	/** A flow of streamed data received from the device. */
	val chunks = _chunks.asSharedFlow()

	/**
	 * Starts the connection to the device and begins observing the Monitoring & Diagnostics Service.
	 *
	 * Use [close] to stop the connection and cancel all observers.
	 */
	fun start() {
		if (job != null) { return }
		job = scope.launch {
			var connection: Job? = null

			// Start observing the central manager state.
			// If Bluetooth gets disabled, or the manager is closed, we cancel the inner scope
			// to cancel all flow observers.
			centralManager.state
				.onEach {
					when (it) {
						Manager.State.POWERED_ON -> {
							// Central Manager is ready, connect or reconnect to the peripheral.
							assert(connection == null) { "Connection already started" }
							val handler = CoroutineExceptionHandler { _, throwable ->
								logger.error(throwable.message)
								_state.update { State.Disconnected(State.Disconnected.Reason.FAILED_TO_CONNECT) }
								cancel()
							}
							connection = launch(handler) {
								connect(centralManager, peripheral)
							}
						}
						Manager.State.UNKNOWN -> {
							// Central Manager was closed, cancel the scope.
							// This will also cancel the connection if it was started.
							cancel()
						}
						else -> {
							// Cancel the connection.
							// It will be restarted when the Central Manager state changes to POWERED_ON.
							connection?.cancel()
							connection = null
						}
					}
				}
				.launchIn(this)

			try { awaitCancellation() }
			finally {
				job = null
			}
		}
	}

	/**
	 * Closes the open connection.
	 *
	 * If the connection is not started, this method does nothing.
	 */
	fun close() {
		job?.cancel()
		job = null
	}

	/**
	 * This method connects the peripheral and starts observing its state.
	 *
	 * It suspends until the scope is canceled.
	 */
	private suspend fun CoroutineScope.connect(
		centralManager: CentralManager,
		peripheral: Peripheral,
	): Nothing {
		// Observe the peripheral bond state to catch bonding failures.
		var wasBonding = false
		peripheral.bondState
			.onEach { bondState ->
				when (bondState) {
					// If bond state transitions from any of these...
					BondState.BONDED,
					BondState.BONDING -> wasBonding = true
					// ... to NONE, it means that the bonding failed.
					BondState.NONE -> {
						if (wasBonding) {
							logger.warn("Bonding failed")
							// This will be reported as bond failure on disconnection.
							bondingFailed = true
							wasBonding = false
						}
					}
				}

			}
			.launchIn(this)

		// Start observing the peripheral state.
		peripheral.state
			// Skip the initial state.
			.drop(1)
			.onEach { state ->
				when (state) {
					// Disconnected state is emitted when the connection is lost, when the device
					// is not supported (disconnect() method called), or the connection was canceled
					// by the user.
					is ConnectionState.Disconnected -> {
						if (state.reason is ConnectionState.Disconnected.Reason.UnsupportedAddress) {
							// This error is thrown in AutoConnect connection when there is no
							// bonding. The library will transition to Direct connection automatically.
							// Don't report this state.
							return@onEach
						}
						_state.emit(state.map(notSupported, bondingFailed))
						if (state.isUserInitiated /* (includes not supported) */ ||
							state.reason is ConnectionState.Disconnected.Reason.UnsupportedConfiguration) {
							// If the disconnection was initiated using disconnect() method,
							// it might have been canceled, or the device is not supported.
							// Either way, cancel auto-reconnection by cancelling the scope.
							cancel()
						}
					}

					// For all other states, we just emit the state.
					// Note, that ConnectionState.Connecting and ConnectionState.Connected
					// emit DeviceState.Connecting state.
					// States DeviceState.Initializing and DeviceState.Connected are emitted later,
					// when the service is initialized. This is to make sure that not supported
					// devices are not reported as connected.
					else -> {
						_state.emit(state.map())
					}
				}
			}
			.launchIn(this)

		// Connect to the peripheral automatically when the manager is created.
		try {
			// If a device is not bonded, but is advertising with resolvable private address (RPA),
			// the AutoConnect option will fail throwing ConnectionFailedException.
			// On older phones it may just hang forever, hence the timeout.
			val isBonded = peripheral.hasBondInformation
			val timeout = if (isBonded) Duration.INFINITE else 5.seconds
			withTimeout(timeout) {
				centralManager.connect(
					peripheral,
					options = CentralManager.ConnectionOptions.AutoConnect(
						automaticallyRequestHighestValueLength = true
					)
				)
			}
		} catch (e: Exception) {
			logger.warn("Connection attempt failed: ${e.message}")

			// Try to connect directly. This should work with RPA.
			centralManager.connect(
				peripheral,
				options = CentralManager.ConnectionOptions.Direct(
					automaticallyRequestHighestValueLength = true
				)
			)
		}

		// Observe the MDS service.
		peripheral.services(listOf(MDS_SERVICE_UUID))
			// services() returns a StateFlow which is initialized with null,
			// indicating that the services are not yet discovered. Filter that out.
			.filterNotNull()
			// Initialize the MDS service on each service changed.
			.onEach { services ->
				// When the MDS service is discovered, it will be the only one in the list.
				val mds = services.firstOrNull()

				// Check if the MDS service is supported.
				// The exception will be caught in the catch block below.
				checkNotNull(mds) { "Monitoring & Diagnostics Service not supported" }

				_state.emit(value = State.Initializing)

				// This method will throw if any of required characteristic is not supported.
				val config = initialize(mds)

				_state.emit(value = State.Connected(config))

				// Make sure the chunks are enabled only after the state changed to Connected.
				start(mds)

				logger.info("Monitoring & Diagnostics Service started successfully")
			}
			.catch { throwable ->
				logger.error(throwable.message)
				// onEach above will throw IllegalStateException if the MDS service is not supported.
				notSupported = throwable is IllegalStateException
				// Some devices return OperationFailedException when bonding fails.
				// This flag is also set by the bond state observer.
				if (throwable is OperationFailedException && throwable.reason == OperationStatus.INSUFFICIENT_AUTHENTICATION) {
					bondingFailed = true
				}
				peripheral.disconnect()
			}
			.launchIn(this)

		try { awaitCancellation() }
		finally {
			// Make sure the device is disconnected when the scope is canceled.
			// When it was already disconnected, this is a no-op.
			withContext(NonCancellable) {
				peripheral.disconnect()

				// The state collection was canceled together with the scope. Emit the state manually.
				_state.emit(peripheral.state.value.map(notSupported, bondingFailed))
			}
		}
	}

	private suspend fun CoroutineScope.initialize(mds: RemoteService): ChunksConfig {
		// Read and emit device configuration.
		val deviceId = mds.deviceIdCharacteristic.read()
			.let { String(it) }
		val url = mds.dataUriCharacteristic.read()
			.let { String(it) }
		val authorisationToken = mds.authorisationCharacteristic.read()
			.let { AuthorisationHeader.parse(it) }

		// Start listening to data collected by the device.
		val deferred = CompletableDeferred<Unit>()
		mds.dataExportCharacteristic
			// Subscribe and enable notifications (on collection).
			.subscribe { deferred.complete(Unit) }
			// This will catch an exception thrown when subscribe fails,
			// i.e. OperationFailedException(reason=Subscribe not permitted)
			.catch { deferred.completeExceptionally(it) }
			.buffer() // TODO is that needed?
			// Emit the chunks to the flow.
			.onEach { _chunks.emit(it) }
			.launchIn(this)

		// Make sure the notifications are enabled and subscribed to before returning.
		deferred.await()

		return ChunksConfig(authorisationToken, url, deviceId)
	}

	private suspend fun start(mds: RemoteService) {
		// Enable notifications for data export characteristic.
		val enableStreamingCommand = byteArrayOf(0x01)
		mds.dataExportCharacteristic.write(enableStreamingCommand, WriteType.WITH_RESPONSE)
	}

	@Suppress("unused")
	private val RemoteService.supportedFeaturesCharacteristic
		get() = characteristics
			.find { it.uuid == MDS_SUPPORTED_FEATURES_CHARACTERISTIC_UUID }
			.let { checkNotNull(it) { "Supported Features characteristic not found" } }
			.also { check(it.properties.contains(CharacteristicProperty.READ)) { "Supported Features characteristic does not have READ property" } }

	private val RemoteService.deviceIdCharacteristic
		get() = characteristics
			.find { it.uuid == MDS_DEVICE_ID_CHARACTERISTIC_UUID }
			.let { checkNotNull(it) { "Device ID characteristic not found" } }
			.also { check(it.properties.contains(CharacteristicProperty.READ)) { "Device ID characteristic does not have READ property" } }

	private val RemoteService.dataUriCharacteristic
		get() = characteristics
			.find { it.uuid == MDS_DATA_URI_CHARACTERISTIC_UUID }
			.let { checkNotNull(it) { "Data URI characteristic not found" } }
			.also { check(it.properties.contains(CharacteristicProperty.READ)) { "Data URI characteristic does not have READ property" } }

	private val RemoteService.authorisationCharacteristic
		get() = characteristics
			.find { it.uuid == MDS_AUTHORISATION_CHARACTERISTIC_UUID }
			.let { checkNotNull(it) { "Authorisation characteristic not found" } }
			.also { check(it.properties.contains(CharacteristicProperty.READ)) { "Authorisation characteristic does not have READ property" } }

	private val RemoteService.dataExportCharacteristic
		get() = characteristics
			.find { it.uuid == MDS_DATA_EXPORT_CHARACTERISTIC_UUID }
			.let { checkNotNull(it) { "Data Export characteristic not found" } }
			.also { check(it.properties.contains(CharacteristicProperty.WRITE)) { "Data Export characteristic does not have WRITE property" } }
			.also { check(it.properties.contains(CharacteristicProperty.NOTIFY)) { "Data Export characteristic does not have NOTIFY property" } }
}