package io.runtime.mcumgr.mock

import io.runtime.mcumgr.McuMgrCallback
import io.runtime.mcumgr.McuMgrErrorCode
import io.runtime.mcumgr.McuMgrHeader
import io.runtime.mcumgr.McuMgrScheme
import io.runtime.mcumgr.McuMgrTransport
import io.runtime.mcumgr.exception.McuMgrException
import io.runtime.mcumgr.mock.handlers.MockStatsHandler
import io.runtime.mcumgr.response.McuMgrResponse
import io.runtime.mcumgr.util.CBOR
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class MockMcuMgrTransport(
    private val statsHandler: MockStatsHandler? = null,
    private val handlerOverrides: List<OverrideHandler> = listOf()
): McuMgrTransport {

    private val executor: Executor = Executors.newSingleThreadExecutor()

    override fun getScheme(): McuMgrScheme {
        return McuMgrScheme.COAP_BLE
    }

    override fun <T : McuMgrResponse?> send(payload: ByteArray, responseType: Class<T>): T {
        val rawHeader = CBOR.getObject(payload, "_h", ByteArray::class.java)
        val header = McuMgrHeader.fromBytes(rawHeader)

        // Check for handler overrides
        handlerOverrides.firstOrNull { handler ->
                handler.groupId == header.groupId && handler.commandId == header.commandId
        }?.let {
            return it.handle(header, payload, responseType)
        }

        // Call defaults
        return when (header.groupId) {
            McuMgrGroup.STATS.value -> statsHandler?.handle(header, payload, responseType) ?:
                buildMockErrorResponse(McuMgrErrorCode.NOT_SUPPORTED, header.toResponse(), responseType)
            else -> buildMockErrorResponse(McuMgrErrorCode.NOT_SUPPORTED, header.toResponse(), responseType)
        }
    }

    override fun <T : McuMgrResponse?> send(
        payload: ByteArray,
        responseType: Class<T>,
        callback: McuMgrCallback<T>
    ) {
        executor.execute {
            try {
                callback.onResponse(send(payload, responseType))
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
        throw IllegalStateException("Not implemented.")

    override fun removeObserver(observer: McuMgrTransport.ConnectionObserver) =
        throw IllegalStateException("Not implemented.")

    override fun release() =
        throw IllegalStateException("Not implemented.")

    override fun connect(callback: McuMgrTransport.ConnectionCallback?) =
        throw IllegalStateException("Not implemented.")
}


