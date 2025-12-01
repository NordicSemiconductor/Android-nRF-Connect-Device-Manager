package no.nordicsemi.android.mcumgr.mock

import no.nordicsemi.android.mcumgr.McuMgrCallback
import no.nordicsemi.android.mcumgr.McuMgrErrorCode
import no.nordicsemi.android.mcumgr.McuMgrHeader
import no.nordicsemi.android.mcumgr.McuMgrScheme
import no.nordicsemi.android.mcumgr.McuMgrTransport
import no.nordicsemi.android.mcumgr.exception.McuMgrException
import no.nordicsemi.android.mcumgr.response.McuMgrResponse
import no.nordicsemi.android.mcumgr.util.CBOR
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class MockCoapMcuMgrTransport(
    private val handler: McuMgrHandler? = null,
    private val handlerOverrides: List<OverrideHandler> = listOf()
): McuMgrTransport {

    private val executor: Executor = Executors.newSingleThreadExecutor()

    override fun getScheme(): McuMgrScheme {
        return McuMgrScheme.COAP_BLE
    }

    override fun <T : McuMgrResponse> send(
        payload: ByteArray,
        timeout: Long,
        responseType: Class<T>
    ): T {
        val rawHeader = CBOR.getObject(payload, "_h", ByteArray::class.java)
        val header = McuMgrHeader.fromBytes(rawHeader)

        // Check for handler overrides
        handlerOverrides.firstOrNull { handler ->
                handler.groupId == header.groupId && handler.commandId == header.commandId
        }?.let {
            return it.handle(header, payload, responseType)
        }

        // Call defaults
        return handler?.handle(header, payload, responseType) ?:
            buildMockErrorResponse(scheme, McuMgrErrorCode.NOT_SUPPORTED, header.toResponse(), responseType)
    }

    override fun <T : McuMgrResponse> send(
        payload: ByteArray,
        timeout: Long,
        responseType: Class<T>,
        callback: McuMgrCallback<T>
    ) {
        executor.execute {
            try {
                callback.onResponse(send(payload, timeout, responseType))
            } catch (mme: McuMgrException) {
                callback.onError(mme)
            } catch (e: Exception) {
                callback.onError(McuMgrException(e))
            }
        }
    }

    /*
     * Unimplemented.
     */
    override fun addObserver(observer: McuMgrTransport.ConnectionObserver) =
        TODO("Not yet implemented")

    override fun removeObserver(observer: McuMgrTransport.ConnectionObserver) =
        TODO("Not yet implemented")

    override fun release() =
        TODO("Not yet implemented")

    override fun connect(callback: McuMgrTransport.ConnectionCallback?) =
        TODO("Not yet implemented")

    override fun changeMode(name: String, callback: McuMgrTransport.ModeChangeCallback?): Boolean {
        TODO("Not yet implemented")
    }
}


