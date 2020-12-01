package io.runtime.mcumgr.transfer

import io.runtime.mcumgr.McuMgrScheme
import kotlinx.coroutines.channels.ReceiveChannel
import java.lang.IllegalArgumentException
import kotlin.math.min
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException

private const val RETRIES = 5

data class UploadProgress(val offset: Int, val size: Int)

abstract class Uploader(
    private val data: ByteArray,
    private val windowCapacity: Int,
    internal var mtu: Int,
    private val protocol: McuMgrScheme
) {

    private val log = LoggerFactory.getLogger("Uploader")

    private val _progress: MutableStateFlow<UploadProgress> =
        MutableStateFlow(UploadProgress(0, data.size))

    val progress: Flow<UploadProgress> = _progress

    @Throws
    internal abstract fun writeAsync(
        data: ByteArray,
        offset: Int,
        length: Int?
    ): ReceiveChannel<UploadResult>

    @Throws
    suspend fun upload() = coroutineScope {
        val window = WindowSemaphore(windowCapacity)
        var offset = 0
        while (offset < data.size) {

            val transmitOffset = offset
            val chunkSize = getChunkSize(data, offset)
            window.acquire()

            log.trace("uploader write: offset=$transmitOffset")

            // Write the chunk asynchronously and launch a coroutine which
            // suspends until the response is received on the result channel.
            val resultChannel = writeChunkAsync(data, transmitOffset, chunkSize)
            launch {
                resultChannel.receive()
                    .mapResponse { response ->
                        // An unexpected offset means that the device did not
                        // accept the chunk. We need to resend the chunk.
                        // Map the response to a failure to be handled by the
                        // onErrorOrFailure block.
                        val responseOffset = response.body.off
                        if (responseOffset != transmitOffset + chunkSize) {
                            val e = IllegalStateException(
                                "Unexpected offset: expected=${transmitOffset + chunkSize}, " +
                                    "actual=${responseOffset}"
                            )
                            UploadResult.Failure(e)
                        } else {
                            response
                        }
                    }
                    .onSuccess {
                        window.success()
                        val current = transmitOffset + chunkSize
                        _progress.value = UploadProgress(current, data.size)
                    }
                    .onErrorOrFailure {
                        log.info("uploader write failure: offset=$transmitOffset")
                        window.fail()
                        // Retry sending the request. Recover the window on success or throw
                        // on failure.
                        retryWriteChunk(data, transmitOffset, chunkSize, RETRIES)
                            .onSuccess {
                                log.info("uploader write recovered: offset=$transmitOffset")
                                window.recover()
                            }
                            .onErrorOrFailure {
                                log.info("uploader write failed: offset=$transmitOffset")
                                throw it
                            }
                    }
                log.trace("uploader write complete: offset=$transmitOffset")
            }

            // Update the offset with the size of the last chunk
            offset += chunkSize
        }
    }

    /**
     * Copy a chunk of data from the offset and send the write request.
     */
    private fun writeChunkAsync(
        data: ByteArray,
        offset: Int,
        chunkSize: Int
    ): ReceiveChannel<UploadResult> {
        val chunk = data.copyOfRange(offset, offset + chunkSize)
        val length = if (offset == 0) {
            data.size
        } else {
            null
        }
        return writeAsync(chunk, offset, length)
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
    ): UploadResult {
        var error: UploadResult? = null
        repeat(times) {
            val result = writeChunkAsync(data, offset, chunkSize).receive()
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
        val headerSize = when (protocol) {
            McuMgrScheme.BLE -> 8
            else -> 8 + 4
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

        // Size of the field name "data" utf8 string
        val dataStringSize = cborStringLength("data")

        val combinedSize = headerSize + mapSize + offsetSize + lengthSize + dataStringSize

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
