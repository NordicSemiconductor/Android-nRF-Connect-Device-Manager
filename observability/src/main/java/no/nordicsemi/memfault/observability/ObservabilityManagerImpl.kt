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

package no.nordicsemi.memfault.observability

import android.content.Context
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.Peripheral
import no.nordicsemi.memfault.observability.bluetooth.DeviceState
import no.nordicsemi.memfault.observability.bluetooth.MonitoringAndDiagnosticsService
import no.nordicsemi.memfault.observability.data.PersistentChunkQueue
import no.nordicsemi.memfault.observability.internal.Scope
import no.nordicsemi.memfault.observability.internet.ChunkManager
import kotlin.time.Duration.Companion.milliseconds

internal class ObservabilityManagerImpl(
    context: Context,
) : ObservabilityManager {
    /** The Application Context. */
    private val context = context.applicationContext

    private val _state = MutableStateFlow(ObservabilityManager.State())
    override val state: StateFlow<ObservabilityManager.State> = _state.asStateFlow()

    private var bleManager: MonitoringAndDiagnosticsService? = null
    private var chunkQueue: PersistentChunkQueue? = null
    private var uploadManager: ChunkManager? = null
    private var job: Job? = null

    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    override fun connect(peripheral: Peripheral, centralManager: CentralManager) {
        check(job == null) { "Already connected to a peripheral" }

        // Start the manager in a new scope.
        job = Scope.launch {
            val scope = this

            // Set up the manager that will collect diagnostic chunks from the device.
            bleManager = MonitoringAndDiagnosticsService(centralManager, peripheral, scope)
                .apply {
                    // Collect the state of the BLE manager and update the state flow.
                    var connection: Job? = null
                    state
                        .onEach { state ->
                            _state.value = _state.value.copy(deviceStatus = state)

                            if (state is DeviceState.Connected) {
                                assert(connection?.isCancelled ?: true) {
                                    "Connection scope should be null or cancelled when the config is received"
                                }

                                connection = scope.launch {
                                    chunkQueue = PersistentChunkQueue(
                                        context = context,
                                        deviceId = state.config.deviceId
                                    ).also { queue ->
                                        queue.chunks
                                            .onEach {
                                                _state.value = _state.value.copy(chunks = it)
                                            }
                                            .launchIn(this)
                                    }
                                    uploadManager = ChunkManager(
                                        config = state.config,
                                        chunkQueue = chunkQueue
                                    ).also { manager ->
                                        manager.status
                                            .onEach {
                                                _state.value = _state.value.copy(uploadingStatus = it)
                                            }
                                            .launchIn(this)
                                        // Upload any chunks that were already in the queue.
                                        manager.uploadChunks()
                                    }

                                    try { awaitCancellation()}
                                    finally {
                                        uploadManager?.uploadChunks()
                                        uploadManager?.close()
                                        uploadManager = null
                                        chunkQueue = null
                                    }
                                }
                            } else {
                                // Otherwise, the device must have been disconnected.
                                connection?.cancel()
                                connection = null
                            }
                        }
                        .onCompletion {
                            // Manager is closing. Clean up and remove all uploaded chunks from the queue.
                            Scope.launch {
                                chunkQueue?.deleteUploaded()
                            }

                            // Cancel the connection job if it is still active.
                            connection?.cancel()
                            connection = null
                        }
                        .launchIn(scope)

                    // Collect the chunks received from the BLE manager and upload them.
                    chunks
                        .onEach {
                            // Mind, that that has to be called from a non-main Dispatcher
                            withContext(NonCancellable) {
                                chunkQueue?.addChunks(listOf(it))
                            }
                        }
                        // Don't upload chunks immediately, but debounce the flow to avoid
                        // multiple uploads in a short time.
                        .debounce(300.milliseconds)
                        .onEach {
                            uploadManager?.uploadChunks()
                        }
                        .launchIn(scope)
                }

            // Start the Bluetooth LE manager to connect to the device and start receiving data.
            bleManager?.start()

            // Wait for disconnection.
            try { awaitCancellation() }
            finally {
                // Clean up the BLE manager when the scope is cancelled.
                bleManager?.close()
                bleManager = null
            }
        }
    }

    override fun disconnect() {
        job?.cancel()
        job = null
    }
}
