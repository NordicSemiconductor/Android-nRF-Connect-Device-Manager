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

package no.nordicsemi.memfault.observability

import no.nordicsemi.memfault.observability.bluetooth.DeviceState
import no.nordicsemi.memfault.observability.data.Chunk
import no.nordicsemi.memfault.observability.data.MemfaultConfig
import no.nordicsemi.memfault.observability.internet.UploadingStatus

/**
 * The state of the Memfault Observability feature.
 *
 * @property deviceStatus The current status of the Bluetooth LE connection.
 * @property uploadingStatus The current status of the uploading process.
 * @property chunks A list of chunks that were received in this session.
 */
data class MemfaultState(
    val deviceStatus: DeviceState = DeviceState.Disconnected(),
    val uploadingStatus: UploadingStatus = UploadingStatus.Idle,
    val chunks: List<Chunk> = emptyList()
) {
    /** Number of chunks that are ready to be uploaded. */
    val pendingChunks: Int = chunks.filter { !it.isUploaded }.size
    /** Total number of bytes uploaded. */
    val bytesUploaded: Int = chunks.filter { it.isUploaded }.sumOf { it.data.size }
    /** The configuration obtained from the device using GATT. */
    val config: MemfaultConfig?
        get() = (deviceStatus as? DeviceState.Connected)?.config
}

