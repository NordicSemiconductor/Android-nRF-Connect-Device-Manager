package com.juul.mcumgr.transfer

import com.juul.mcumgr.McuMgrResult
import com.juul.mcumgr.getOrElse
import com.juul.mcumgr.getOrThrow
import com.juul.mcumgr.onSuccess
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.Buffer

private const val RETRIES = 5

abstract class Downloader(windowCapacity: Int) {

    private val window = WindowSemaphore(windowCapacity)
    private val _progress: MutableStateFlow<Progress?> = MutableStateFlow(null)

    val progress: Flow<Progress> = _progress.filterNotNull()

    data class Response(val data: ByteArray, val offset: Int, val length: Int?)
    data class Progress(val offset: Int, val size: Int)

    abstract suspend fun read(offset: Int): McuMgrResult<Response>

    @Throws
    suspend fun download(): ByteArray = coroutineScope {
        var offset = 0

        // Initial read to initialize output buffer and expected size
        val initialResponse = read(0)
            .getOrElse { retryRead(0, RETRIES).getOrThrow() }

        val length = checkNotNull(initialResponse.length) { "length missing from initial response" }
        val expectedSize = initialResponse.data.size

        val buffer = Buffer().write(initialResponse.data)
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
                    response.data.copyTo(buffer, transmitOffset)
                }
                _progress.value = Progress(transmitOffset + response.data.size, length)
            }

            // Update the offset with the size of the last chunk
            offset += expectedSize
        }

        buffer.readByteArray()
    }
}

/**
 * Retry sending an download read request.
 *
 * Returns the last received error if all attempts fail.
 */
private suspend fun Downloader.retryRead(
    offset: Int,
    times: Int
): McuMgrResult<Downloader.Response> {
    var error: McuMgrResult<Downloader.Response>? = null
    repeat(times) {
        val result = read(offset)
        when {
            result.isSuccess -> return result
            result.isFailure -> error = result
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
    if (actualSize != expectedSize && offset + actualSize != totalSize) {
        throw IllegalStateException("data size does not match expected size")
    }
}

private fun ByteArray.copyTo(buffer: Buffer, offset: Int) {
    Buffer().write(this).copyTo(buffer, offset.toLong())
}
