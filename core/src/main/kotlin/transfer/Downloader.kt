package com.juul.mcumgr.transfer

import com.juul.mcumgr.McuManager
import com.juul.mcumgr.McuMgrResult
import com.juul.mcumgr.getOrElse
import com.juul.mcumgr.getOrThrow
import com.juul.mcumgr.map
import com.juul.mcumgr.onSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

private const val RETRIES = 5

interface Downloader {

    suspend fun read(offset: Int): McuMgrResult<Response>

    data class Event(val data: ByteArray, val offset: Int, val length: Int)
    data class Response(val data: ByteArray, val offset: Int, val length: Int?)
}

@Throws
fun Downloader.flow(
    capacity: Int
) = channelFlow {
    val window = WindowSemaphore(capacity)
    var offset = 0

    // Initial read to initialize output buffer and expected size
    val initialResponse = read(0)
        .getOrElse { retryRead(0, RETRIES).getOrThrow() }
    val length = checkNotNull(initialResponse.length) { "length missing from initial response" }
    val expectedSize = initialResponse.data.size

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
            send(Downloader.Event(response.data, transmitOffset, length))
        }

        // Update the offset with the size of the last chunk
        offset += expectedSize
    }
}

suspend fun Downloader.readAll(capacity: Int): ByteArray =
    flow(capacity).collectBytes()

suspend fun Flow<Downloader.Event>.collectBytes(): ByteArray {
    var buffer: ByteArray? = null
    collect { event ->
        if (buffer == null) {
            buffer = ByteArray(event.length)
        }
        val b = checkNotNull(buffer) { "buffer cannot be null" }
        event.data.copyInto(b, event.offset)
    }
    return checkNotNull(buffer) { "buffer cannot be null" }
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

// Downloader Implementations

class CoreDownloader(val manager: McuManager) : Downloader {

    override suspend fun read(offset: Int): McuMgrResult<Downloader.Response> =
        manager.coreDownload(offset).map { response ->
            Downloader.Response(response.data, response.offset, response.length)
        }
}

class FileDownloader(val manager: McuManager) : Downloader {

    override suspend fun read(offset: Int): McuMgrResult<Downloader.Response> =
        manager.fileDownload(offset).map { response ->
            Downloader.Response(response.data, response.offset, response.length)
        }
}
