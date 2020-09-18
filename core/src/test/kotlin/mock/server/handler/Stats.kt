package mock.server.handler

import com.juul.mcumgr.command.Command
import com.juul.mcumgr.command.Group
import com.juul.mcumgr.command.ResponseCode
import com.juul.mcumgr.serialization.Message
import mock.server.payloadMap
import mock.server.toResponse
import util.getNotNull

class StatsReadHandler : Handler {

    override val group = Group.Stats
    override val command = Command.Stats.Read
    override val accept = readOnly

    var stats: MutableMap<String, MutableMap<String, Long>> = mutableMapOf()

    override fun handle(message: Message): Message {
        val payload = message.payloadMap
        val name: String = payload.getNotNull("name")
        val group = stats[name] ?: return message.toResponse(ResponseCode.NoEntry)
        val responsePayload: Map<String, Any> = mapOf(
            "name" to name,
            "fields" to group
        )
        return message.toResponse(payload = responsePayload)
    }
}

class StatsListHandler : Handler {

    override val group = Group.Stats
    override val command = Command.Stats.List
    override val accept = readOnly

    var groups: List<String> = mutableListOf()

    override fun handle(message: Message): Message {
        val responsePayload: Map<String, Any> = mapOf("stat_list" to groups)
        return message.toResponse(payload = responsePayload)
    }
}
