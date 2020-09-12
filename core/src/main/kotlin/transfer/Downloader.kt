package com.juul.mcumgr.transfer

import com.juul.mcumgr.SendResult
import com.juul.mcumgr.getOrElse
import com.juul.mcumgr.getOrThrow
import com.juul.mcumgr.onSuccess
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val RETRIES = 5

data class DownloadProgress(val offset: Int, val size: Int)

abstract class Downloader(private val windowCapacity: Int) {

    private val _progress: MutableStateFlow<DownloadProgress?> = MutableStateFlow(null)

    val progress: Flow<DownloadProgress> = _progress.filterNotNull()

    data class Response(val data: ByteArray, val offset: Int, val length: Int?)

    abstract suspend fun read(offset: Int): SendResult<Response>

    @Throws
    suspend fun download(): ByteArray = coroutineScope {
        val window = WindowSemaphore(windowCapacity)
        var offset = 0

        // Initial read to initialize output buffer and expected size
        val initialResponse = read(0)
            .getOrElse { retryRead(0, RETRIES).getOrThrow() }

        val expectedSize = initialResponse.data.size
        val length = checkNotNull(initialResponse.length) {
            "length missing from initial response"
        }

        val data = ByteArray(length)
        initialResponse.data.copyInto(data, 0)
        val bufferMutex = Mutex()

        while (offset < length) {
            val transmitOffset = offset
            window.acquire()
            launch {
                val response = read(transmitOffset)
                    .onSuccess { window.success() }
                    .getOrElse {
                        window.fail()
                        // Retry sending the request. Recover the window on success or throw
                        // on failure.
                        retryRead(transmitOffset, RETRIES)
                            .onSuccess { window.recover() }
                            .getOrThrow()
                    }

                // Assert the data size is as expected and send the event
                assertDataSize(response.data.size, expectedSize, transmitOffset, length)
                bufferMutex.withLock {
                    response.data.copyInto(data, transmitOffset)
                }
                _progress.value = DownloadProgress(transmitOffset + response.data.size, length)
            }

            // Update the offset with the size of the last chunk
            offset += expectedSize
        }

        data
    }

    /**
     * Retry sending an download read request.
     *
     * Returns the last received error if all attempts fail.
     */
    private suspend fun retryRead(
        offset: Int,
        times: Int
    ): SendResult<Response> {
        var error: SendResult<Response>? = null
        repeat(times) {
            val result = read(offset)
            when {
                result.isSuccess -> return result
                result.isError || result.isFailure -> error = result
            }
        }
        return checkNotNull(error)
    }

    /**
     * Validate the the data size is as expected.
     *
     * Handles the case of the last data packet usually being smaller than the expected size.
     */
    private fun assertDataSize(actualSize: Int, expectedSize: Int, offset: Int, totalSize: Int) {
        check(actualSize == expectedSize || offset + actualSize == totalSize) {
            "actual size $actualSize differs from exepcted size $expectedSize"
        }
    }
}
