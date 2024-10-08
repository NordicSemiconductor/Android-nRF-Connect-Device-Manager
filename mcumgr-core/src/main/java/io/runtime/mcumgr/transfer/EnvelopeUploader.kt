package io.runtime.mcumgr.transfer

import io.runtime.mcumgr.McuMgrCallback
import io.runtime.mcumgr.exception.McuMgrException
import io.runtime.mcumgr.managers.SUITManager
import io.runtime.mcumgr.response.suit.McuMgrUploadResponse

private const val OP_WRITE = 2
private const val ID_ENVELOPE_UPLOAD = 2

/**
 * This uploader is using a [SUITManager] to upload the SUIT Envelope.
 *
 * The SUIT Envelope is a CBOR-encoded data structure that contains the manifest and, optionally,
 * the firmware. The SUIT Envelope is sent to the device to start the firmware update process.
 * If the device decides, during the processing, that additional resources are required, it will
 * notify the polling client using the [SUITManager.poll] method.
 *
 * The response the the poll operation contains a resource URI and session ID. The resource
 * should be sent using [ResourceUploader].
 *
 * @property suitManager The SUIT Manager.
 * @param envelope The candidate SUIT Envelope to be sent, as bytes.
 * @param windowCapacity Number of buffers available for sending data, defaults to 1. The more buffers
 * are available, the more packets can be sent without awaiting notification with response, thus
 * accelerating upload process.
 * @param memoryAlignment The memory alignment of the device, defaults to 1. Some memory
 * implementations may require bytes to be aligned to a certain value before saving them.
 */
open class EnvelopeUploader(
    private val suitManager: SUITManager,
    envelope: ByteArray,
    windowCapacity: Int = 1,
    memoryAlignment: Int = 1,
    private val deferInstall: Boolean = false,
) : Uploader(
    envelope,
    windowCapacity,
    memoryAlignment,
    suitManager.mtu,
    suitManager.scheme
) {
    override fun getAdditionalSize(offset: Int): Int =
        // "defer_install": 0x6D64656665725F696E7374616C6C + 0xF5 (true)
        if (offset == 0 && deferInstall) 15 else 0

    override fun getAdditionalData(data: ByteArray, offset: Int, map: MutableMap<String, Any>) {
        if (offset == 0 && deferInstall) {
            map["defer_install"] = true
        }
    }

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
