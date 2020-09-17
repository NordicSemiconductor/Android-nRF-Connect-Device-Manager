package mock.server

import com.juul.mcumgr.Protocol
import com.juul.mcumgr.ResponseCode
import com.juul.mcumgr.serialization.Message
import com.juul.mcumgr.serialization.decode
import com.juul.mcumgr.serialization.encode
import mock.server.handler.Handler
import mock.server.handler.defaultHandlers

class Server(
    private val mtu: Int,
    private val protocol: Protocol
) {

    val overrides: MutableList<Handler> = mutableListOf()
    var handlers: List<Handler> = defaultHandlers

    fun reset() {
        overrides.clear()
        handlers = defaultHandlers
    }

    fun handle(requestData: ByteArray): ByteArray {

        if (requestData.size > mtu) {
            error("request data size ${requestData.size} is larger than mtu $mtu")
        }

        val message = requestData.decode(protocol)

        // First check the override then default handlers. If none exist, return NotSupported error
        val handler = overrides.findHandler(message) ?: handlers.findHandler(message)
            ?: return message.toResponse(ResponseCode.NotSupported).encode(protocol)

        val response = handler.handle(message)
        return response.encode(protocol)
    }

    inline fun <reified T : Handler> findHandler(): T =
        overrides.firstOrNull { it is T } as T?
            ?: handlers.firstOrNull { it is T } as T? ?: error("handler ${T::class} not found")

    private fun List<Handler>.findHandler(message: Message): Handler? {
        return find { handler ->
            message.header.group == handler.group.value &&
                message.header.command == handler.command.value &&
                handler.accept.map { it.value }.contains(message.header.operation)
        }
    }
}
