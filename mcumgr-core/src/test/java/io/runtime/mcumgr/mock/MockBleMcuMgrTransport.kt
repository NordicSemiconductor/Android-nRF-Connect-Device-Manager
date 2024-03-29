package io.runtime.mcumgr.mock

import io.runtime.mcumgr.McuMgrCallback
import io.runtime.mcumgr.McuMgrErrorCode
import io.runtime.mcumgr.McuMgrHeader
import io.runtime.mcumgr.McuMgrScheme
import io.runtime.mcumgr.McuMgrTransport
import io.runtime.mcumgr.exception.McuMgrException
import io.runtime.mcumgr.response.McuMgrResponse
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class MockBleMcuMgrTransport(
    private val handler: McuMgrHandler? = null,
    private val handlerOverrides: List<OverrideHandler> = listOf()
): McuMgrTransport {

    private val executor: Executor = Executors.newSingleThreadExecutor()

    override fun getScheme(): McuMgrScheme {
        return McuMgrScheme.BLE
    }

    override fun <T : McuMgrResponse> send(
        data: ByteArray,
        timeout: Long,
        responseType: Class<T>
    ): T {
        val header = McuMgrHeader.fromBytes(data)
        val payload = data.drop(McuMgrHeader.HEADER_LENGTH).toByteArray()

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
        throw IllegalStateException("Not implemented.")

    override fun removeObserver(observer: McuMgrTransport.ConnectionObserver) =
        throw IllegalStateException("Not implemented.")

    override fun release() =
        throw IllegalStateException("Not implemented.")

    override fun connect(callback: McuMgrTransport.ConnectionCallback?) =
        throw IllegalStateException("Not implemented.")
}


