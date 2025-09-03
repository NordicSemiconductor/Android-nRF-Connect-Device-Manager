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

@file:Suppress("unused")

package no.nordicsemi.android.observability

import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.flow.StateFlow
import no.nordicsemi.android.observability.bluetooth.MonitoringAndDiagnosticsService
import no.nordicsemi.android.observability.bluetooth.MonitoringAndDiagnosticsService.State.Connected
import no.nordicsemi.android.observability.bluetooth.MonitoringAndDiagnosticsService.State.Disconnected
import no.nordicsemi.android.observability.data.Chunk
import no.nordicsemi.android.observability.data.ChunksConfig
import no.nordicsemi.android.observability.internal.Scope
import no.nordicsemi.android.observability.internet.ChunkManager
import no.nordicsemi.android.observability.internet.ChunkManager.State.Idle
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.Peripheral
import no.nordicsemi.kotlin.ble.client.android.native

/**
 * Class responsible for managing connection with the remote IoT device which supports
 * Monitoring & Diagnostic Service.
 *
 * The manager connects to the device and uploads all downloaded chunks to the cloud.
 *
 * Data can be emitted any time so the connection should be maintained as long as needed.
 *
 * @see <a href="https://app.memfault.com">Console</a>
 * @see <a href="https://docs.memfault.com/docs/mcu/mds">Monitoring & Diagnostic GATT Service</a>
 */
interface ObservabilityManager {

    /**
     * The state of the nRF Cloud Observability feature.
     *
     * @property state The current status of the Bluetooth LE connection.
     * @property uploadingState The current status of the uploading process.
     * @property chunks A list of chunks that were received in this session.
     */
    data class State(
        val state: MonitoringAndDiagnosticsService.State = Disconnected(),
        val uploadingState: ChunkManager.State = Idle,
        val chunks: List<Chunk> = emptyList()
    ) {
        /** Number of chunks that are ready to be uploaded. */
        val chunksPending: Int = chunks.filter { !it.isUploaded }.size
        /** Number of chunks uploaded to the cloud. */
        val chunksUploaded: Int = chunks.filter { it.isUploaded }.size
        /** Total number of bytes of pending chunks. */
        val bytesPending: Int = chunks.filter { !it.isUploaded }.sumOf { it.data.size }
        /** Total number of bytes uploaded. */
        val bytesUploaded: Int = chunks.filter { it.isUploaded }.sumOf { it.data.size }
        /** The configuration obtained from the device using GATT.
         *
         * This is only available when the device is connected and MDS service was read successfully.
         */
        val config: ChunksConfig?
            get() = (state as? Connected)?.config
    }

    /**
     * The state of the manager.
     *
     * Contains all the information exposed by the library like:
     *  - Bluetooth connection status with the selected IoT device.
     *  - Uploading status which may be suspended due to lack of Internet connection or server overload.
     *  - Chunks information.
     */
    val state: StateFlow<State>

    /**
     * Function used to connect to the selected Bluetooth LE peripheral.
     *
     * The peripheral must support Monitoring & Diagnostic Service.
     *
     * Chunks upload will start immediately after establishing the connection.
     *
     * This method allows using mock central manager, which is useful for testing purposes.
     *
     * Calling this method with an already connected peripheral will throw an exception.
     *
     * @param peripheral [Peripheral] to which the manager should connect.
     * @param centralManager [CentralManager] to use to connect to the peripheral.
     * @throws IllegalStateException if the manager is already connected to a peripheral.
     */
    fun connect(peripheral: Peripheral, centralManager: CentralManager)

    /**
     * Function used to connect to the selected Bluetooth LE peripheral.
     *
     * The peripheral must support Monitoring & Diagnostic Service.
     *
     * Chunks upload will start immediately after establishing the connection.
     *
     * @param context Android [Context] need to initialize the chunks database.
     * @param device [BluetoothDevice] to which the manager should connect.
     * @throws IllegalStateException if the manager is already connected to a peripheral.
     */
    fun connect(context: Context, device: BluetoothDevice) {
        val centralManager = CentralManager.Factory.native(context, Scope)
        val peripheral = centralManager.getPeripheralById(device.address)!!
        connect(peripheral, centralManager)
    }

    /**
     * Disconnects the connected peripheral.
     */
    fun disconnect()

    companion object {

        /**
         * This function creates a new instance of [ObservabilityManager].
         *
         * @param context Android [Context] need to initialize the chunks database.
         * @return new [ObservabilityManager] instance
         */
        @JvmStatic
        fun create(context: Context): ObservabilityManager {
            return ObservabilityManagerImpl(context)
        }
    }
}
