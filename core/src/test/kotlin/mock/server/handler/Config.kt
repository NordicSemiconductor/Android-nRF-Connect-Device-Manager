package mock.server.handler

import com.juul.mcumgr.command.Command
import com.juul.mcumgr.command.Group
import com.juul.mcumgr.command.Operation
import com.juul.mcumgr.command.ResponseCode
import com.juul.mcumgr.serialization.Message
import mock.server.payloadMap
import mock.server.toResponse
import util.getNotNull

class ConfigHandler : Handler {

    override val group = Group.Config
    override val command = Command.Config.Config
    override val accept = readWrite

    var config: MutableMap<String, String> = mutableMapOf()

    override fun handle(message: Message): Message {
        return when (message.header.operation) {
            Operation.Read.value -> handleRead(message)
            Operation.Write.value -> handleWrite(message)
            else -> error("can only handle requests")
        }
    }

    private fun handleRead(message: Message): Message {
        val payload = message.payloadMap
        val name: String = payload.getNotNull("name")
        val value = config[name] ?: return message.toResponse(ResponseCode.InValue)
        val responsePayload: Map<String, Any> = mapOf("val" to value)
        return message.toResponse(payload = responsePayload)
    }

    private fun handleWrite(message: Message): Message {
        val payload = message.payloadMap
        val name: String = payload.getNotNull("name")
        val value: String = payload.getNotNull("val")
        val save: Boolean = payload.getNotNull("save")

        if (config[name] == null) {
            return message.toResponse(ResponseCode.InValue)
        }
        config[name] = value

        return message.toResponse()
    }
}
