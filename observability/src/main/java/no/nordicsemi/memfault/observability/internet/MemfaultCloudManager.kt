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

package no.nordicsemi.memfault.observability.internet

import com.memfault.cloud.sdk.ChunkQueue
import com.memfault.cloud.sdk.ChunkSender
import com.memfault.cloud.sdk.MemfaultCloud
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import no.nordicsemi.memfault.observability.data.MemfaultConfig
import no.nordicsemi.memfault.observability.data.PersistentChunkQueue
import java.net.URL
import kotlin.time.Duration.Companion.seconds

/**
 * Manages the upload of chunks to the Memfault cloud.
 *
 * By default, the [ChunkQueue] lives in memory. If the application is closed or
 * crashes with chunks un-sent, they will be lost.
 *
 * If this is an issue for your application, you can provide the [ChunkQueue] that is persistent,
 * for example [PersistentChunkQueue].
 *
 * @param config Configuration for Memfault, including AAuthorization Token and device ID (serial).
 * @param chunkQueue A queue for storing chunks to be uploaded. If `null`, a default in-memory
 * queue will be used. Use [PersistentChunkQueue] for persistent storage.
 */
class MemfaultCloudManager(
    config: MemfaultConfig,
    chunkQueue: ChunkQueue? = null,
) {
    private val _status = MutableStateFlow<UploadingStatus>(UploadingStatus.Idle)
    val status = _status.asStateFlow()

    /**
     * The Memfault Cloud object is used to access the Memfault API.
     *
     * [Documentation](https://github.com/memfault/memfault-cloud-android)
     */
    private val memfaultCloud: MemfaultCloud = MemfaultCloud.Builder()
        .setApiKey(config.authorisationToken)
        .apply {
            // The URL in Memfault Config may contain the full path to the Chunks endpoint.
            // Memfault Cloud object requires only the base URL.
            baseChunksUrl = URL(config.url)
                .let { URL(it.protocol, it.host, "") }
                .toString()
        }
        .build()

    /**
     * The sender is responsible for sending chunks to the Memfault Cloud.
     */
    private val memfaultSender: ChunkSender = ChunkSender.Builder()
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
        // Check if the Memfault Cloud object was deinitialized.
        if (closed) {
            return@coroutineScope
        }

        // If the state is suspended, we should not start a new upload.
        // It will resume automatically when the delay is over.
        if (status.value is UploadingStatus.Suspended) {
            return@coroutineScope
        }
        // Change the status to InProgress to indicate that the upload is starting.
        _status.value = UploadingStatus.InProgress

        // Push the chunks from the queue to the Memfault Cloud.
        val result = memfaultSender.send()
        when (result) {
            is ChunkSenderResult.Success -> {
                _status.value = UploadingStatus.Idle
            }
            is ChunkSenderResult.Error -> {
                retryAfter(result.delayInSeconds)
            }
        }
    }

    /**
     * Suspends the upload for a given number of seconds.
     *
     * This method will update the status to [UploadingStatus.Suspended] each second
     * with the remaining time until the upload can be resumed.
     */
    private suspend fun retryAfter(numberOfSeconds: Long) {
        for (i in numberOfSeconds downTo 0) {
            _status.value = UploadingStatus.Suspended(i)
            delay(1.seconds)

            // Stop the loop if the manager is closed.
            if (closed) { return }
        }

        // Retry the upload after the delay.
        _status.value = UploadingStatus.InProgress
        uploadChunks()
    }

    /**
     * Closes the uploader.
     */
    fun close() {
        closed = true
        memfaultCloud.deinit()
        _status.value = UploadingStatus.Idle
    }
}
