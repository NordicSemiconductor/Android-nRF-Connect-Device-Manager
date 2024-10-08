package io.runtime.mcumgr.transfer

import io.runtime.mcumgr.McuMgrCallback
import io.runtime.mcumgr.exception.McuMgrException
import io.runtime.mcumgr.managers.SUITManager
import io.runtime.mcumgr.response.suit.McuMgrUploadResponse
import io.runtime.mcumgr.util.CBOR

private const val OP_WRITE = 2
private const val ID_CACHE_RAW_UPLOAD = 5

/**
 * This uploader is using a [SUITManager] to upload the cache file during device firmware update.
 *
 * After sending a SUIT Envelope using [EnvelopeUploader] with _deferred install_, use this
 * uploader to send the cache files.
 *
 * @property suitManager The SUIT Manager.
 * @property partition The target partition ID.
 * @param data The resource data, as bytes.
 * @param windowCapacity Number of buffers available for sending data, defaults to 1. The more buffers
 * are available, the more packets can be sent without awaiting notification with response, thus
 * accelerating upload process.
 * @param memoryAlignment The memory alignment of the device, defaults to 1. Some memory
 * implementations may require bytes to be aligned to a certain value before saving them.
 */
open class CacheUploader(
    private val suitManager: SUITManager,
    private val partition: Int,
    data: ByteArray,
    windowCapacity: Int = 1,
    memoryAlignment: Int = 1,
) : Uploader(
    data,
    windowCapacity,
    memoryAlignment,
    suitManager.mtu,
    suitManager.scheme
) {
    override fun write(requestMap: Map<String, Any>, timeout: Long, callback: (UploadResult) -> Unit) {
        suitManager.uploadAsync(requestMap, timeout, callback)
    }

    override fun getAdditionalData(
        data: ByteArray,
        offset: Int,
        map: MutableMap<String, Any>
    ) {
        if (offset == 0) {
            map["target_id"] = partition
        }
    }

    override fun getAdditionalSize(offset: Int): Int =
        // "target_id": 0x697461726765745F6964 + partition ID
        if (offset == 0) 18 + CBOR.uintLength(partition) else 0
}

private fun SUITManager.uploadAsync(
    requestMap: Map<String, Any>,
    timeout: Long,
    callback: (UploadResult) -> Unit
) = send(OP_WRITE, ID_CACHE_RAW_UPLOAD, requestMap, timeout, McuMgrUploadResponse::class.java,
    object : McuMgrCallback<McuMgrUploadResponse> {
        override fun onResponse(response: McuMgrUploadResponse) {
            callback(UploadResult.Response(response, response.returnCode))
        }

        override fun onError(error: McuMgrException) {
            callback(UploadResult.Failure(error))
        }
    }
)