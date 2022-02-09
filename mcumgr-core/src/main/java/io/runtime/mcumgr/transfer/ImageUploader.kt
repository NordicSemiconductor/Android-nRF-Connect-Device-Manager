package io.runtime.mcumgr.transfer

import io.runtime.mcumgr.McuMgrCallback
import io.runtime.mcumgr.exception.InsufficientMtuException
import io.runtime.mcumgr.exception.McuMgrException
import io.runtime.mcumgr.managers.ImageManager
import io.runtime.mcumgr.response.UploadResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException

private const val OP_WRITE = 2
private const val ID_UPLOAD = 1

fun ImageManager.windowUpload(
    data: ByteArray,
    image: Int,
    windowCapacity: Int,
    callback: UploadCallback
): TransferController {
    val log = LoggerFactory.getLogger("ImageUploader")
    val uploader = ImageUploader(data, image, this, windowCapacity)

    val exceptionHandler = CoroutineExceptionHandler { _, t ->
        log.error("Upload failed", t)
    }
    val job = GlobalScope.launch(exceptionHandler) {
        val progress = uploader.progress.onEach { progress ->
            callback.onUploadProgressChanged(
                progress.offset,
                progress.size,
                System.currentTimeMillis()
            )
        }.launchIn(this)

        uploader.uploadCatchMtu()
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
        override fun pause() = throw IllegalStateException("cannot pause window upload")
        override fun resume() = throw IllegalStateException("cannot resume window upload")
        override fun cancel() {
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
    private val imageData: ByteArray,
    private val image: Int,
    private val imageManager: ImageManager,
    windowCapacity: Int = 1
) : Uploader(
    imageData,
    windowCapacity,
    imageManager.mtu,
    imageManager.scheme
) {

    override fun write(data: ByteArray, offset: Int, callback: (UploadResult) -> Unit) {
        val requestMap: MutableMap<String, Any> = mutableMapOf(
            "data" to data,
            "off" to offset
        )
        if (offset == 0) {
            if (image > 0) {
                requestMap["image"] = image
            }
            requestMap["len"] = imageData.size
            // TODO "sha" is not supported by the Uploader, as it's sending multiple chunks in parallel without waiting for the first response.
        }
        imageManager.uploadAsync(requestMap, callback)
    }

    override fun getAdditionalSize(): Int {
        if (image > 0) {
            return super.getAdditionalSize() + cborStringLength("image") + cborUIntLength(image)
        }
        return super.getAdditionalSize()
    }
}

private fun ImageManager.uploadAsync(
    requestMap: Map<String, Any>,
    callback: (UploadResult) -> Unit
) = send(OP_WRITE, ID_UPLOAD, requestMap, UploadResponse::class.java,
    object : McuMgrCallback<UploadResponse> {
        override fun onResponse(response: UploadResponse) {
            callback(UploadResult.Response(response, response.returnCode))
        }

        override fun onError(error: McuMgrException) {
            callback(UploadResult.Failure(error))
        }
    }
)
