package io.runtime.mcumgr.transfer

import io.runtime.mcumgr.McuMgrScheme
import io.runtime.mcumgr.exception.InsufficientMtuException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import kotlin.math.min

const val MAX_CHUNK_FAILURES = 5

data class UploadProgress(
    val offset: Int,
    val size: Int,
    val timestamp: Long = System.currentTimeMillis()
)

private data class Chunk(val data: ByteArray, val offset: Int) {
    override fun toString(): String {
        return "Chunk(offset=$offset, size=${data.size})"
    }
}

abstract class Uploader(
    private val data: ByteArray,
    private val windowCapacity: Int,
    private val memoryAlignment: Int,
    internal var mtu: Int,
    private val protocol: McuMgrScheme
) {
    private val log = LoggerFactory.getLogger("Uploader")

    private val _progress: MutableSharedFlow<UploadProgress> = MutableSharedFlow(
        replay = 2,
        extraBufferCapacity = 2,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private var currentOffset = 0

    val progress: Flow<UploadProgress> = _progress
    private val resumed = Semaphore(1)

    /**
     * This method should send the request with given parameters.
     */
    @Throws
    internal abstract fun write(
        requestMap: Map<String, Any>,
        callback: (UploadResult) -> Unit
    )

    /**
     * Uploads the data.
     */
    @Throws
    suspend fun upload() = coroutineScope {
        // Tracks the number of failures experienced for any given chunk,
        // identified by the offset.
        val failureDirectory = mutableMapOf<Int, Int>()
        val failureDirectoryMutex = Mutex()

        // Bounds number of in-progress requests within window capacity.
        val window = Semaphore(windowCapacity)

        val next: Channel<Chunk> = Channel(CONFLATED)
        val failures: Channel<Chunk> = Channel(CONFLATED)
        val close: Channel<Unit> = Channel(CONFLATED)

        val initialTimestamp = System.currentTimeMillis()
        next.send(newChunk(0))

        while (true) {
            window.acquire()

            // Try acquiring resumed lock. If worked, release it immediately.
            resumed.acquire()
            resumed.release()

            // Select the next chunk to send, prioritizing failed chunks.
            val (chunk, resend) = select<Pair<Chunk, Boolean>?> {
                failures.onReceive { it to true }
                next.onReceive { it to false }
                close.onReceive { null }
            } ?: break

            val nextChunk = writeInternal(chunk, resend, this) { result ->
                result.onSuccess { response ->
                    if (!resend && response.off < chunk.offset + chunk.data.size) {
                        // An unexpected offset means that the message was
                        // somehow lost or the device could not accept the
                        // chunk. We need to resend the chunk at the offset
                        // requested by the device.
                        log.warn("Chunk with offset ${chunk.offset} has been lost (expected offset=${chunk.offset + chunk.data.size}, received=${response.off})")
                        failures.send(newChunk(response.off))
                    } else {
                        // Success, update the progress.
                        if (chunk.offset == 0 && response.off == chunk.data.size) {
                            _progress.tryEmit(UploadProgress(0, data.size, initialTimestamp))
                        }
                        if (currentOffset < response.off) {
                            _progress.tryEmit(UploadProgress(response.off, data.size))
                            currentOffset = response.off
                        }
                        if (response.off == data.size) {
                            close.send(Unit)
                        }
                    }
                }.onErrorOrFailure { failure ->
                    if (failure is InsufficientMtuException) {
                        throw failure
                    }
                    // Request failure, resend failed chunk.
                    log.warn("Uploader write failure for chunk with offset ${chunk.offset}: $failure")
                    // Track the number of times a chunk has failed. If the
                    // chunk has failed more times than the threshold,
                    // throw the exception to fail the upload entirely.
                    val fails = failureDirectoryMutex.withLock {
                        val fails = (failureDirectory[chunk.offset] ?: 0) + 1
                        failureDirectory[chunk.offset] = fails
                        fails
                    }
                    if (fails >= MAX_CHUNK_FAILURES) {
                        throw failure
                    }
                    failures.send(newChunk(chunk.offset))
                }

                // Release the semaphore.
                window.release()
            }

            // Only send the next chunk if the we still have more data to upload.
            if (nextChunk.offset < data.size) {
                next.send(nextChunk)
            }
        }
    }

    /**
     * Pauses upload.
     */
    suspend fun pause() {
        resumed.acquire()
    }

    /**
     * Resumes upload.
     */
    fun resume() {
        resumed.release()
    }

    private suspend fun writeInternal(
        chunk: Chunk,
        resend: Boolean,
        scope: CoroutineScope,
        callback: suspend (UploadResult) -> Unit
    ): Chunk {
        val resultChannel: Channel<UploadResult> = Channel(1)
        write(prepareWrite(chunk.data, chunk.offset)) { result ->
            resultChannel.trySend(result)
        }

        return if (chunk.offset == 0) {
            // Send the first chunk synchronously, to get the last offset.
            val result = resultChannel.receive()
            callback(result)

            result.onSuccess {
                return newChunk(it.off)
            }
            return nextChunk(chunk)
        } else if (resend) {
            // Failed and resent chunks should suspend the current coroutine
            // and await the result.
            val result = resultChannel.receive()
            callback(result)

            // When the result is successful response with an offset, return
            // a new chunk with the requested offset.
            when (result) {
                is UploadResult.Response -> {
                    newChunk(result.body.off)
                }
                else -> nextChunk(chunk)
            }
        } else {
            // Regular send should launch the result handling on a child coroutine.
            scope.launch {
                val result = resultChannel.receive()
                callback(result)
            }

            // Return the next logical chunk.
            nextChunk(chunk)
        }
    }

    private fun newChunk(offset: Int): Chunk {
        // SMP pipelining may require data to be aligned to some number of bytes.
        // In Zephyr, since https://github.com/zephyrproject-rtos/zephyr/pull/41959 has been merged
        // this is not required, but memory aligning here makes even older devices to work.
        val maxChunkSize = getMaxChunkSize(data, offset)
        val alignedSize =
            if (offset + maxChunkSize < data.size) maxChunkSize / memoryAlignment * memoryAlignment else maxChunkSize
        val chunkData = data.copyOfRange(offset, offset + alignedSize)
        return Chunk(chunkData, offset)
    }

    private fun nextChunk(chunk: Chunk): Chunk {
        return newChunk(chunk.offset + chunk.data.size)
    }

    private fun prepareWrite(
        data: ByteArray,
        offset: Int,
    ): Map<String, Any> = mutableMapOf<String, Any>(
        "data" to data,
        "off" to offset
    ).also {
        if (offset == 0) {
            it["len"] = this.data.size // NOT data.size, as data is just a chunk of this.data
        }
        getAdditionalData(data, offset, it)
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
    private fun getMaxChunkSize(data: ByteArray, offset: Int): Int {
        // The size of the header is based on the scheme. CoAP scheme is larger because there are
        // 4 additional bytes of CBOR.
        val headerSize = when (protocol) {
            McuMgrScheme.BLE -> 8
            else -> 8 + 4
        }

        // Size of the indefinite length map tokens (bf, ff)
        val mapSize = 2

        // Size of the field name "data" utf8 string
        val dataStringSize = cborStringLength("data")

        // Size of the string "off" plus the length of the offset integer
        val offsetSize = cborStringLength("off") + cborUIntLength(offset)

        // Size of the string "len" plus the length of the data size integer
        // "len" is sent only in the initial packet.
        val lengthSize = if (offset == 0) {
            cborStringLength("len") + cborUIntLength(data.size)
        } else {
            0
        }

        // Implementation specific size
        val implSpecificSize = getAdditionalSize(offset)

        val combinedSize = headerSize + mapSize + offsetSize + lengthSize + implSpecificSize + dataStringSize

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

    /**
     * This method should add additional parameters to the map.
     * The "data", "len" and "off" parameters are already added.
     */
    internal open fun getAdditionalData(
        data: ByteArray,
        offset: Int,
        map: MutableMap<String, Any>
    ) {
        // Empty default implementation.
    }

    /**
     * This method should return an additional size of the CBOR payload, which will be placed to the
     * packet with the given offset.
     * The "data", "len" and "off" parameters are already calculated.
     */
    internal open fun getAdditionalSize(offset: Int) = 0
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
