package io.runtime.mcumgr.transfer

import io.runtime.mcumgr.McuMgrCallback
import io.runtime.mcumgr.exception.McuMgrException
import io.runtime.mcumgr.managers.SUITManager
import io.runtime.mcumgr.response.suit.McuMgrUploadResponse

private const val OP_WRITE = 2
private const val ID_ENVELOPE_UPLOAD = 2

open class EnvelopeUploader(
    private val suitManager: SUITManager,
    envelope: ByteArray,
    windowCapacity: Int = 1,
    memoryAlignment: Int = 1,
) : Uploader(
    envelope,
    windowCapacity,
    memoryAlignment,
    suitManager.mtu,
    suitManager.scheme
) {
    override fun write(requestMap: Map<String, Any>, timeout: Long, callback: (UploadResult) -> Unit) {
        suitManager.uploadAsync(requestMap, timeout, callback)
    }
}

private fun SUITManager.uploadAsync(
    requestMap: Map<String, Any>,
    timeout: Long,
    callback: (UploadResult) -> Unit
) = send(OP_WRITE, ID_ENVELOPE_UPLOAD, requestMap, timeout, McuMgrUploadResponse::class.java,
    object : McuMgrCallback<McuMgrUploadResponse> {
        override fun onResponse(response: McuMgrUploadResponse) {
            callback(UploadResult.Response(response, response.returnCode))
        }

        override fun onError(error: McuMgrException) {
            callback(UploadResult.Failure(error))
        }
    }
)
