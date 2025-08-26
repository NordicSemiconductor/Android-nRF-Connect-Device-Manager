package no.nordicsemi.android.mcumgr.transfer

import no.nordicsemi.android.mcumgr.McuMgrCallback
import no.nordicsemi.android.mcumgr.exception.McuMgrException
import no.nordicsemi.android.mcumgr.managers.FsManager
import no.nordicsemi.android.mcumgr.response.UploadResponse
import no.nordicsemi.android.mcumgr.util.CBOR

private const val OP_WRITE = 2
private const val ID_FILE = 0

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
        map["name"] = name
    }

    override fun getAdditionalSize(offset: Int): Int =
        CBOR.stringLength("name") + CBOR.stringLength(name)
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
