package io.runtime.mcumgr.transfer

import io.runtime.mcumgr.McuMgrCallback
import io.runtime.mcumgr.exception.McuMgrException
import io.runtime.mcumgr.managers.FsManager
import io.runtime.mcumgr.response.UploadResponse
import io.runtime.mcumgr.util.CBOR

private const val OP_WRITE = 2
private const val ID_FILE = 1

open class FileUploader(
    private val fsManager: FsManager,
    private val name: String,
    data: ByteArray,
    windowCapacity: Int = 1,
    memoryAlignment: Int = 1,
) : Uploader(
    data,
    windowCapacity,
    memoryAlignment,
    fsManager.mtu,
    fsManager.scheme
) {
    override fun write(requestMap: Map<String, Any>, timeout: Long, callback: (UploadResult) -> Unit) {
        fsManager.uploadAsync(requestMap, timeout, callback)
    }

    override fun getAdditionalData(
        data: ByteArray,
        offset: Int,
        map: MutableMap<String, Any>
    ) {
        map.takeIf { offset == 0 }?.apply {
            put("name", name)
        }
    }

    override fun getAdditionalSize(offset: Int): Int = when (offset) {
        // "name" param is only sent in the first packet.
        0 -> CBOR.stringLength("name") + CBOR.stringLength(name)
        else -> 0
    }
}

private fun FsManager.uploadAsync(
    requestMap: Map<String, Any>,
    timeout: Long,
    callback: (UploadResult) -> Unit
) = send(OP_WRITE, ID_FILE, requestMap, timeout, UploadResponse::class.java,
    object : McuMgrCallback<UploadResponse> {
        override fun onResponse(response: UploadResponse) {
            callback(UploadResult.Response(response, response.returnCode))
        }

        override fun onError(error: McuMgrException) {
            callback(UploadResult.Failure(error))
        }
    }
)
