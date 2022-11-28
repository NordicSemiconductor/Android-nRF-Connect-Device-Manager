package io.runtime.mcumgr.transfer

import io.runtime.mcumgr.McuMgrCallback
import io.runtime.mcumgr.exception.InsufficientMtuException
import io.runtime.mcumgr.exception.McuMgrException
import io.runtime.mcumgr.managers.ImageManager
import io.runtime.mcumgr.response.UploadResponse
import io.runtime.mcumgr.util.CBOR
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

private const val OP_WRITE = 2
private const val ID_UPLOAD = 1
private const val TRUNCATED_HASH_LEN = 3

fun ImageManager.windowUpload(
    data: ByteArray,
    image: Int,
    windowCapacity: Int,
    memoryAlignment: Int,
    callback: UploadCallback
): TransferController {
    val log = LoggerFactory.getLogger("ImageUploader")
    val uploader = ImageUploader(data, image, this, windowCapacity, memoryAlignment)

    val exceptionHandler = CoroutineExceptionHandler { _, t ->
        log.error("Upload failed", t)
    }
    val job = GlobalScope.launch(exceptionHandler) {
        val progress = uploader.progress.onEach { progress ->
            callback.onUploadProgressChanged(
                progress.offset,
                progress.size,
                progress.timestamp,
            )
        }.launchIn(this)

        val start = System.currentTimeMillis()
        uploader.uploadCatchMtu()
        val duration = System.currentTimeMillis() - start
        log.info("Upload completed. ${data.size} bytes sent in $duration ms with avg speed: ${data.size.toFloat() / (duration.toFloat() + 1f)} kBytes/s") // + 1 to prevent division by zero
        progress.cancel()
    }

    job.invokeOnCompletion { throwable ->
        throwable?.printStackTrace()
        when (throwable) {
            null -> callback.onUploadCompleted()
            is CancellationException -> callback.onUploadCanceled()
            is McuMgrException -> callback.onUploadFailed(throwable)
            else -> callback.onUploadFailed(McuMgrException(throwable))
        }
    }

    return object : TransferController {
        var paused: Job? = null

        override fun pause() {
            paused = GlobalScope.launch { uploader.pause() }
        }
        override fun resume() {
            uploader.resume()
            paused = null
        }
        override fun cancel() {
            paused?.cancel()
            job.cancel()
        }
    }
}

// Catches an mtu exception, sets the new mtu and restarts the upload.
private suspend fun Uploader.uploadCatchMtu() {
    try {
        upload()
    } catch (e: InsufficientMtuException) {
        mtu = e.mtu
        upload()
    }
}

internal class ImageUploader(
    imageData: ByteArray,
    private val image: Int,
    private val imageManager: ImageManager,
    windowCapacity: Int = 1,
    memoryAlignment: Int = 1,
) : Uploader(
    imageData,
    windowCapacity,
    memoryAlignment,
    imageManager.mtu,
    imageManager.scheme
) {
    override fun write(requestMap: Map<String, Any>, timeout: Long, callback: (UploadResult) -> Unit) {
        imageManager.uploadAsync(requestMap, timeout, callback)
    }

    override fun getAdditionalData(
        data: ByteArray,
        offset: Int,
        map: MutableMap<String, Any>
    ) {
        map.takeIf { offset == 0 }?.apply {
            takeIf { image > 0 }?.let { put("image", image) }
            sha(data)?.let { put("sha", it) }
        }
    }

    override fun getAdditionalSize(offset: Int): Int = when (offset) {
        // "sha" and "image" params are only sent in the first packet.
        0 -> {
            val shaSize =
                CBOR.stringLength("sha") + CBOR.uintLength(TRUNCATED_HASH_LEN) + TRUNCATED_HASH_LEN
            val imageSize = if (image > 0) {
                CBOR.stringLength("image") + CBOR.uintLength(image)
            } else 0
            shaSize + imageSize
        }
        else -> 0
    }

    /**
     * This method should return a session identifier for the given data.
     * In theory, this can be any string, but should be derived from the data, so that different
     * byte arrays produce a different string, but the same array returns an equal one.
     * This allows to resume uploading the previously started image in case the new and old
     * identifiers match, or start a new session if a different identifiers is sent.
     */
    private fun sha(data: ByteArray): ByteArray? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(data)
            // Truncate the hash to save space.
            Arrays.copyOf(hash, TRUNCATED_HASH_LEN)
        } catch (e: NoSuchAlgorithmException) {
            null
        }
    }
}

private fun ImageManager.uploadAsync(
    requestMap: Map<String, Any>,
    timeout: Long,
    callback: (UploadResult) -> Unit
) = send(OP_WRITE, ID_UPLOAD, requestMap, timeout, UploadResponse::class.java,
    object : McuMgrCallback<UploadResponse> {
        override fun onResponse(response: UploadResponse) {
            callback(UploadResult.Response(response, response.returnCode))
        }

        override fun onError(error: McuMgrException) {
            callback(UploadResult.Failure(error))
        }
    }
)
