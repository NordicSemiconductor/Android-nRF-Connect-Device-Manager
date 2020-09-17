package mock.server.handler

import com.juul.mcumgr.Command
import com.juul.mcumgr.Group
import com.juul.mcumgr.command.System
import com.juul.mcumgr.dateToString
import com.juul.mcumgr.serialization.Message
import com.juul.mcumgr.stringToDate
import mock.server.payloadMap
import mock.server.toResponse
import util.getNotNull
import java.util.Date
import java.util.TimeZone

class EchoHandler : Handler {

    override val group = Group.System
    override val command = Command.System.Echo
    override val accept = readWrite

    override fun handle(message: Message): Message {
        val payload = message.payloadMap
        val echo: String = payload.getNotNull("d")
        val responsePayload = mapOf("r" to echo)
        return message.toResponse(payload = responsePayload)
    }
}

class ConsoleEchoControlHandler : Handler {

    override val group = Group.System
    override val command = Command.System.ConsoleEchoControl
    override val accept = writeOnly

    var enabled: Boolean = false

    override fun handle(message: Message): Message {
        val payload = message.payloadMap
        enabled = payload.getNotNull("echo")
        return message.toResponse()
    }
}

class TaskStatsHandler : Handler {

    override val group = Group.System
    override val command = Command.System.TaskStats
    override val accept = readOnly

    var taskStats: MutableMap<String, System.TaskStatsResponse.Task> = mutableMapOf()

    override fun handle(message: Message): Message {
        val responsePayload = mapOf("tasks" to taskStats)
        return message.toResponse(payload = responsePayload)
    }
}

class MemoryPoolStatsHandler : Handler {

    override val group = Group.System
    override val command = Command.System.MemoryPoolStats
    override val accept = readOnly

    var memoryPoolStats: MutableMap<String, System.MemoryPoolStatsResponse.MemoryPool> = mutableMapOf()

    override fun handle(message: Message): Message {
        val responsePayload = mapOf("mpools" to memoryPoolStats)
        return message.toResponse(payload = responsePayload)
    }
}

class ReadDatetimeHandler : Handler {

    override val group = Group.System
    override val command = Command.System.Datetime
    override val accept = readOnly

    var date: Date = Date()

    override fun handle(message: Message): Message {
        val responsePayload =
            mapOf("datetime" to dateToString(date, TimeZone.getTimeZone("UTC")))
        return message.toResponse(payload = responsePayload)
    }
}

class WriteDatetimeHandler : Handler {

    override val group = Group.System
    override val command = Command.System.Datetime
    override val accept = writeOnly

    var date: Date = Date()

    override fun handle(message: Message): Message {
        val payload = message.payloadMap
        date = stringToDate(payload.getNotNull("datetime"))
        return message.toResponse()
    }
}

class ResetHandler : Handler {

    override val group = Group.System
    override val command = Command.System.Reset
    override val accept = writeOnly

    override fun handle(message: Message): Message {
        return message.toResponse()
    }
}
