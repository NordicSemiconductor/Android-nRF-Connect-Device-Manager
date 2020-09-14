package mock.server

import com.juul.mcumgr.serialization.Command
import com.juul.mcumgr.command.Protocol
import com.juul.mcumgr.serialization.Group
import com.juul.mcumgr.serialization.Operation
import com.juul.mcumgr.command.ResponseCode
import com.juul.mcumgr.serialization.Message
import com.juul.mcumgr.serialization.decode
import com.juul.mcumgr.serialization.encode
import kotlinx.coroutines.delay

class Server(
    val mtu: Int,
    val protocol: Protocol,
    val overrides: MutableList<Handler> = mutableListOf(),
    val delay: Long = 0
) {

    val handlers: List<Handler> = defaultHandlers

    suspend fun handle(requestData: ByteArray): ByteArray {

        if (requestData.size > mtu) {
            error("request data size ${requestData.size} is larger than mtu $mtu")
        }

        val message = requestData.decode(protocol)

        // First check the override then default handlers. If none exist, return NotSupported error
        val handler = overrides.findHandler(message) ?: handlers.findHandler(message)
            ?: return message.toResponse(ResponseCode.NotSupported).encode(protocol)

        val response = handler.handle(message)
        delay(delay)
        return response.encode(protocol)
    }

    private fun List<Handler>.findHandler(message: Message): Handler? {
        return find { handler ->
            message.header.group == handler.group.value &&
                message.header.command == handler.command.value &&
                handler.accept.map { it.value }.contains(message.header.operation)
        }
    }

    inline fun <reified T: Handler> findHandler(): T =
        overrides.firstOrNull { it is T } as T? ?:
            handlers.firstOrNull { it is T } as T? ?: error("handler ${T::class} not found")


    fun findHandler(
        operation: Operation,
        group: Group,
        command: Command
    ): Handler? {
        return overrides.findHandler(operation, group, command)
            ?: handlers.findHandler(operation, group, command)
    }

    private fun List<Handler>.findHandler(
        operation: Operation,
        group: Group,
        command: Command
    ): Handler? {
        return find { handler ->
            group == handler.group &&
                command == handler.command &&
                handler.accept.contains(operation)
        }
    }
}
