package com.juul.mcumgr.transfer

import com.juul.mcumgr.McuManager
import com.juul.mcumgr.McuMgrResult
import com.juul.mcumgr.Transport
import com.juul.mcumgr.map
import com.juul.mcumgr.onFailure
import com.juul.mcumgr.onSuccess
import java.lang.IllegalArgumentException
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

internal const val TRUNCATED_HASH_LEN = 3
private const val RETRIES = 5

interface Uploader {

    val mtu: Int
    val scheme: Transport.Scheme

    suspend fun write(data: ByteArray, offset: Int, length: Int): McuMgrResult<Response>

    data class Event(val offset: Int, val length: Int)
    data class Response(val offset: Int)
}

@Throws
suspend fun Uploader.flow(
    data: ByteArray,
    capacity: Int
) = channelFlow {
    val window = WindowSemaphore(capacity)
    var offset = 0

    while (offset < data.size) {

        val transmitOffset = offset
        val chunkSize = getChunkSize(data, offset)

        window.acquire()

        launch {
            write(data, transmitOffset, chunkSize)
                .onSuccess {
                    window.success()
                    val current = transmitOffset + chunkSize
                    send(Uploader.Event(current, data.size))
                }
                .onFailure {
                    window.fail()
                    // Retry sending the request. Recover the window on success or throw
                    // on failure.
                    retryWrite(data, transmitOffset, chunkSize, RETRIES)
                        .onSuccess { window.recover() }
                        .onFailure { throw it }
                }
        }

        // Update the offset with the size of the last chunk
        offset += chunkSize
    }
}

/**
 * Retry sending an upload write request.
 *
 * Returns the last received error if all attempts fail.
 */
private suspend fun Uploader.retryWrite(
    data: ByteArray,
    offset: Int,
    length: Int,
    times: Int
): McuMgrResult<Uploader.Response> {
    var error: McuMgrResult<Uploader.Response>? = null
    repeat(times) {
        val result = write(data, offset, length)
        when {
            result.isSuccess -> return result
            result.isFailure -> error = result
        }
    }
    return checkNotNull(error)
}

/**
 * Returns the maximum amount of upload data which can fit into an upload request.
 *
 * This calculation is optimal, and takes into account the transport scheme and size of data and
 * offset since CBOR will make the integers as efficient as possible.
 */
private fun Uploader.getChunkSize(data: ByteArray, offset: Int): Int {

    // The size of the header is based on the scheme. CoAP scheme is larger because there are
    // 4 additional bytes of CBOR.
    val headerSize = when (scheme) {
        Transport.Scheme.Standard -> 8
        Transport.Scheme.CoAP -> 8 + 4
    }

    // Size of the indefinite length map tokens (bf, ff)
    val mapSize = 2

    // Size of the string "off" plus the length of the offset integer
    val offsetSize = 4 + cborUIntLen(offset)

    // Size of the string "len" plus the length of the data size integer
    val lengthSize = 4 + cborUIntLen(data.size)

    // Size of the string "sha" plus the length of the truncated hash
    val shaSize = 4 + 1 + TRUNCATED_HASH_LEN

    // Size of the field name "data" utf8 string
    val dataStringSize = 5

    val combinedSize = headerSize + mapSize + offsetSize + lengthSize + shaSize + dataStringSize

    // Now we calculate the max amount of data that we can fit given the MTU.
    val maxDataLength = mtu - combinedSize
    // We have to take into account the few bytes of CBOR which describe the length of the data.
    // Even though we don't know the actual length at this point, the maxDataLength is guaranteed
    // to be larger than what we will eventually send, making this calculation always correct.
    val maxDataUIntTokenSize = cborUIntLen(maxDataLength)

    // Final data chunk size
    return mtu - combinedSize - maxDataUIntTokenSize
}

/**
 * Calculates the size in bytes of an unsigned integer encoded as CBOR.
 */
private fun cborUIntLen(n: Int): Int =
    when {
        n < 0 -> throw IllegalArgumentException("n must be >= 0")
        n < 24 -> 1
        n < 256 -> 2        // 2^8
        n < 65536 -> 3      // 2^16
        n < 4294967296 -> 5 // 2^32
        else -> 9
    }

// Uploader Implementations

class ImageUploader(val manager: McuManager) : Uploader {

    override val mtu: Int = manager.transport.mtu
    override val scheme: Transport.Scheme = manager.transport.scheme

    override suspend fun write(
        data: ByteArray,
        offset: Int,
        length: Int
    ): McuMgrResult<Uploader.Response> =
        manager.imageUpload(data, offset, length).map { response ->
            Uploader.Response(response.offset)
        }
}

class FileUploader(val manager: McuManager) : Uploader {

    override val mtu: Int = manager.transport.mtu
    override val scheme: Transport.Scheme = manager.transport.scheme

    override suspend fun write(
        data: ByteArray,
        offset: Int,
        length: Int
    ): McuMgrResult<Uploader.Response> =
        manager.fileUpload(data, offset, length).map { response ->
            Uploader.Response(response.offset)
        }
}
