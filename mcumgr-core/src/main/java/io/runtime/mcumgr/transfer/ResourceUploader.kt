package io.runtime.mcumgr.transfer

import io.runtime.mcumgr.McuMgrCallback
import io.runtime.mcumgr.exception.McuMgrException
import io.runtime.mcumgr.managers.SUITManager
import io.runtime.mcumgr.response.suit.McuMgrUploadResponse
import io.runtime.mcumgr.util.CBOR

private const val OP_WRITE = 2
private const val ID_RESOURCE_UPLOAD = 4

/**
 * This uploader is using a [SUITManager] to upload the resource requested by the device during
 * device firmware update.
 *
 * After sending a SUIT Envelope using [EnvelopeUploader], use [SUITManager.poll] to check if any
 * resource is requested.
 */
open class ResourceUploader(
    private val suitManager: SUITManager,
    private val sessionId: Int,
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
            map["stream_session_id"] = sessionId
        }
    }

    override fun getAdditionalSize(offset: Int): Int = when (offset) {
        // "stream_session_id": 0x73747265616D5F73657373696F6E5F6964 (18 bytes) + session ID
        0 -> 18 + CBOR.uintLength(sessionId)
        else -> 0
    }
}

private fun SUITManager.uploadAsync(
    requestMap: Map<String, Any>,
    timeout: Long,
    callback: (UploadResult) -> Unit
) = send(OP_WRITE, ID_RESOURCE_UPLOAD, requestMap, timeout, McuMgrUploadResponse::class.java,
    object : McuMgrCallback<McuMgrUploadResponse> {
        override fun onResponse(response: McuMgrUploadResponse) {
            callback(UploadResult.Response(response, response.returnCode))
        }

        override fun onError(error: McuMgrException) {
            callback(UploadResult.Failure(error))
        }
    }
)
