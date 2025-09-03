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

package no.nordicsemi.android.observability.internet

import com.memfault.cloud.sdk.ChunkQueue
import com.memfault.cloud.sdk.ChunkSender
import com.memfault.cloud.sdk.MemfaultCloud
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import no.nordicsemi.android.observability.data.ChunksConfig
import no.nordicsemi.android.observability.data.PersistentChunkQueue
import no.nordicsemi.android.observability.internal.ChunkSenderResult
import no.nordicsemi.android.observability.internal.send
import java.net.URL
import kotlin.time.Duration.Companion.seconds

/**
 * Manages the upload of chunks to nRF Cloud Observability service.
 *
 * By default, the [ChunkQueue] lives in memory. If the application is closed or
 * crashes with chunks un-sent, they will be lost.
 *
 * If this is an issue for your application, you can provide the [ChunkQueue] that is persistent,
 * for example [PersistentChunkQueue].
 *
 * @param config Device configuration which includes Authorization Token and device ID (serial number).
 * @param chunkQueue A queue for storing chunks to be uploaded. If `null`, a default in-memory
 * queue will be used. Use [PersistentChunkQueue] for persistent storage using.
 * @property status A [StateFlow] representing the current status of the manager.
 */
class ChunkManager(
    config: ChunksConfig,
    chunkQueue: ChunkQueue? = null,
) {
    /**
     * Status of the manager.
     *
     * The manager is in [Idle] state when there are no chunks to upload.
     * It switches to [InProgress] when uploading chunks and to [Suspended] when
     * an error occurs and the upload is suspended for a certain delay.
     */
    sealed interface State {
        /** The chunks are not uploaded at the moment. */
        data object Idle : State

        /** The chunks are currently being uploaded. */
        data object InProgress : State

        /**
         * Uploading chunks has been suspended.
         *
         * This status may be used when the Internet connection is lost or when the server
         * is not reachable.
         *
         * @property delayInSeconds The delay in seconds before the next attempt to upload chunks.
         */
        data class Suspended(val delayInSeconds: Long) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val status = _state.asStateFlow()

    /**
     * The Memfault Cloud object is used to access the Memfault API.
     *
     * [Documentation](https://github.com/memfault/memfault-cloud-android)
     */
    private val memfaultCloud: MemfaultCloud = MemfaultCloud.Builder()
        .setApiKey(config.authorisationToken)
        .apply {
            // The URL in device configuration may contain the full path to the Chunks endpoint.
            // Memfault Cloud object requires only the base URL.
            baseChunksUrl = URL(config.url)
                .let { URL(it.protocol, it.host, "") }
                .toString()
        }
        .build()

    /**
     * The sender is responsible for sending chunks to the cloud.
     */
    private val sender: ChunkSender = ChunkSender.Builder()
        .setMemfaultCloud(memfaultCloud)
        .setDeviceSerialNumber(config.deviceId)
        .apply {
            chunkQueue?.let { setChunkQueue(it) }
        }
        .build()
    /** A flag indicating whether the manager is closed. */
    private var closed = false

    /**
     * Uploads already collected chunks to the cloud.
     *
     * It should be triggered in 3 scenarios:
     * 1. After successful connection when the chunk queue is not empty.
     * 2. After received chunks.
     * 3. After suspended delay got from previous upload.
     */
    suspend fun uploadChunks(): Unit = coroutineScope {
        // Check if the manager was deinitialized.
        if (closed) {
            return@coroutineScope
        }

        // If the state is suspended, we should not start a new upload.
        // It will resume automatically when the delay is over.
        if (status.value is State.Suspended) {
            return@coroutineScope
        }
        // Change the status to InProgress to indicate that the upload is starting.
        _state.value = State.InProgress

        // Push the chunks from the queue to the cloud.
        val result = sender.send()
        when (result) {
            is ChunkSenderResult.Success -> {
                _state.value = State.Idle
            }
            is ChunkSenderResult.Error -> {
                retryAfter(result.delayInSeconds)
            }
        }
    }

    /**
     * Suspends the upload for a given number of seconds.
     *
     * This method will update the status to [State.Suspended] each second
     * with the remaining time until the upload can be resumed.
     */
    private suspend fun retryAfter(numberOfSeconds: Long) {
        for (i in numberOfSeconds downTo 0) {
            _state.value = State.Suspended(i)
            delay(1.seconds)

            // Stop the loop if the manager is closed.
            if (closed) { return }
        }

        // Retry the upload after the delay.
        _state.value = State.InProgress
        uploadChunks()
    }

    /**
     * Closes the uploader.
     */
    fun close() {
        closed = true
        memfaultCloud.deinit()
        _state.value = State.Idle
    }
}
