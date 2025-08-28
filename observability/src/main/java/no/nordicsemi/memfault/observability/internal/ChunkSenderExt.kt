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

package no.nordicsemi.memfault.observability.internal

import com.memfault.cloud.sdk.ChunkSender
import com.memfault.cloud.sdk.SendChunksCallback
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Sends the chunk to nRF Cloud.
 *
 * This is suspended version of [ChunkSender.send].
 *
 * See [PostChunksTask](https://github.com/memfault/memfault-cloud-android/blob/4c7aae35bf013071977f618d512165c8e91739d4/sdk/src/main/java/com/memfault/cloud/sdk/internal/PostChunksTask.kt#L8).
 */
internal suspend fun ChunkSender.send() = suspendCoroutine {
    send(object : SendChunksCallback {
        /**
         * All chunks were successfully sent.
         */
        override fun onQueueEmpty(sent: Int) {
            it.resume(ChunkSenderResult.Success(sent))
        }

        /**
         * The server was busy or an error occurred, please re-try after a minimum delay.
         *
         * @param delay The delay after which the [ChunkSender] should try to the chunks again, in
         * seconds. This number increases with each retry.
         * @param sent The number of chunks successfully sent prior to the error.
         * @param exception The error that occurred.
         */
        override fun onRetryAfterDelay(delay: Long, sent: Int, exception: Exception) {
            it.resume(ChunkSenderResult.Error(delay, sent, exception))
        }
    })
}

/**
 * Result of the chunk upload operation.
 *
 * This is used to report the result of sending chunks to the server.
 */
internal sealed interface ChunkSenderResult {
    /**
     * All chunks were successfully sent.
     *
     * @property sent the number of chunks that were sent.
     */
    data class Success(val sent: Int) : ChunkSenderResult

    /**
     * The server was busy or an error occurred, please re-try after a minimum delay.
     *
     * @property delayInSeconds The delay after which the [ChunkSender] should try to the
     * chunks again, in seconds. This number increases with each retry.
     * @property sent The number of chunks successfully sent prior to the error.
     * @property exception The error that occurred.
     */
    data class Error(
        val delayInSeconds: Long,
        val sent: Int,
        val exception: Exception
    ) : ChunkSenderResult
}
