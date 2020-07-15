package com.juul.mcumgr.transfer

import com.juul.mcumgr.McuMgrResult
import com.juul.mcumgr.message.Format
import com.juul.mcumgr.onErrorOrFailure
import com.juul.mcumgr.onSuccess
import java.lang.IllegalArgumentException
import kotlin.math.min
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal const val TRUNCATED_HASH_LEN = 3
private const val RETRIES = 5

abstract class Uploader(
    private val data: ByteArray,
    windowCapacity: Int,
    private val mtu: Int,
    private val format: Format,
    private val packetOverhead: Int
) {

    private val window = WindowSemaphore(windowCapacity)
    private val _progress: MutableStateFlow<Progress> =
        MutableStateFlow(Progress(0, data.size))

    val progress: Flow<Progress> = _progress

    data class Response(val offset: Int)
    data class Progress(val offset: Int, val size: Int)

    abstract suspend fun write(
        data: ByteArray,
        offset: Int,
        length: Int?
    ): McuMgrResult<Response>

    @Throws
    suspend fun upload() = coroutineScope {

        var offset = 0

        while (offset < data.size) {

            val transmitOffset = offset
            val chunkSize = getChunkSize(data, offset)

            window.acquire()

            launch {
                writeChunk(data, transmitOffset, chunkSize)
                    .onSuccess {
                        window.success()
                        val current = transmitOffset + chunkSize
                        _progress.value = Progress(current, data.size)
                    }
                    .onErrorOrFailure {
                        window.fail()
                        // Retry sending the request. Recover the window on success or throw
                        // on failure.
                        retryWriteChunk(data, transmitOffset, chunkSize, RETRIES)
                            .onSuccess { window.recover() }
                            .onErrorOrFailure { throw it }
                    }
            }

            // Update the offset with the size of the last chunk
            offset += chunkSize
        }
    }

    /**
     * Copy a chunk of data from the offset and send the write request.
     */
    private suspend fun writeChunk(
        data: ByteArray,
        offset: Int,
        chunkSize: Int
    ): McuMgrResult<Response> {
        val chunk = data.copyOfRange(offset, offset + chunkSize)
        val length = if (offset == 0) {
            data.size
        } else {
            null
        }
        return write(chunk, offset, length)
    }

    /**
     * Retry sending an upload write request.
     *
     * Returns the last received error if all attempts fail.
     */
    private suspend fun retryWriteChunk(
        data: ByteArray,
        offset: Int,
        chunkSize: Int,
        times: Int
    ): McuMgrResult<Response> {
        var error: McuMgrResult<Response>? = null
        repeat(times) {
            val result = writeChunk(data, offset, chunkSize)
            when {
                result.isSuccess -> return result
                result.isError || result.isFailure -> error = result
            }
        }
        return checkNotNull(error)
    }

    /**
     * Returns the maximum amount of upload data which can fit into an upload request with the given
     * data and offset.
     *
     * This calculation is optimal, and takes into account the transport scheme and size of data and
     * offset since CBOR will make the integers as efficient as possible. In order to avoid an index
     * out of bounds on the last chunk, if the calculated chunk size is greater than data.size -
     * offset, then the latter value is returned.
     */
    private fun getChunkSize(data: ByteArray, offset: Int): Int {

        // The size of the header is based on the scheme. CoAP scheme is larger because there are
        // 4 additional bytes of CBOR.
        val headerSize = when (format) {
            Format.SMP -> 8
            Format.OMP -> 8 + 4
        }

        // Size of the indefinite length map tokens (bf, ff)
        val mapSize = 2

        // Size of the string "off" plus the length of the offset integer
        val offsetSize = cborStringLength("off") + cborUIntLength(offset)

        val lengthSize = if (offset == 0) {
            // Size of the string "len" plus the length of the data size integer
            cborStringLength("len") + cborUIntLength(data.size)
        } else {
            0
        }

        // Size of the string "sha" plus the length of the truncated hash
        val shaSize = if (offset == 0) {
            cborStringLength("sha") + 1 + TRUNCATED_HASH_LEN
        } else {
            0
        }

        // Size of the field name "data" utf8 string
        val dataStringSize = cborStringLength("data")

        val combinedSize = headerSize + mapSize + offsetSize + lengthSize + shaSize +
            dataStringSize + packetOverhead

        // Now we calculate the max amount of data that we can fit given the MTU.
        val maxDataLength = mtu - combinedSize
        // We have to take into account the few bytes of CBOR which describe the length of the data.
        // Even though we don't know the actual length at this point, the maxDataLength is guaranteed
        // to be larger than what we will eventually send, making this calculation always correct.
        val maxDataUIntTokenSize = cborUIntLength(maxDataLength)

        // Final data chunk size
        val maxChunkSize = mtu - combinedSize - maxDataUIntTokenSize
        return min(maxChunkSize, data.size - offset)
    }
}

/**
 * Calculates the size in bytes of a CBOR encoded string.
 */
internal fun cborStringLength(s: String): Int {
    val headerLength = cborUIntLength(s.length)
    return headerLength + s.length
}

/**
 * Calculates the size in bytes of a CBOR encoded unsigned integer.
 */
internal fun cborUIntLength(n: Int): Int =
    when {
        n < 0 -> throw IllegalArgumentException("n must be >= 0")
        n < 24 -> 1
        n < 256 -> 2        // 2^8
        n < 65536 -> 3      // 2^16
        n < 4294967296 -> 5 // 2^32
        else -> 9
    }
