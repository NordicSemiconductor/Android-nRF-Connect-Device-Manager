package mock.server

import com.juul.mcumgr.command.MemoryPoolStatsResponse
import com.juul.mcumgr.command.ResponseCode
import com.juul.mcumgr.command.TaskStatsResponse
import com.juul.mcumgr.dateToString
import com.juul.mcumgr.serialization.Command
import com.juul.mcumgr.serialization.Group
import com.juul.mcumgr.serialization.Message
import com.juul.mcumgr.serialization.Operation
import com.juul.mcumgr.stringToDate
import java.util.Date
import java.util.TimeZone
import kotlin.math.min

private val readOnly: Set<Operation> = setOf(Operation.Read)
private val writeOnly: Set<Operation> = setOf(Operation.Write)
private val readWrite: Set<Operation> = setOf(Operation.Read, Operation.Write)

/**
 * Handles a request with a specific group, command, and operation.
 */
interface Handler {

    val group: Group
    val command: Command
    val accept: Set<Operation>

    fun handle(message: Message): Message
}

val defaultHandlers: List<Handler> = listOf(
    // System
    EchoHandler(),
    ConsoleEchoControlHandler(),
    TaskStatsHandler(),
    MemoryPoolStatsHandler(),
    ReadDatetimeHandler(),
    WriteDatetimeHandler(),
    ResetHandler(),
    // Image
    ImageWriteHandler(),
    CoreReadHandler(),
    // File
    FileWriteHandler(),
    FileReadHandler()
)

/*
 * Error Handlers
 */

fun Handler.toErrorResponseHandler(code: ResponseCode): ErrorResponseHandler {
    return ErrorResponseHandler(code, group, command)
}

fun Handler.toThrowHandler(throwable: Throwable): ThrowHandler {
    return ThrowHandler(throwable, group, command)
}

class ErrorResponseHandler(
    val code: ResponseCode,
    override val group: Group,
    override val command: Command
) : Handler {
    override val accept = readWrite

    override fun handle(message: Message): Message {
        return message.toResponse(code)
    }
}

class ThrowHandler(
    val throwable: Throwable,
    override val group: Group,
    override val command: Command
) : Handler {

    override val accept = readWrite

    override fun handle(message: Message): Message {
        throw throwable
    }
}

/*
 * System Handlers
 */

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

    var taskStats: MutableMap<String, TaskStatsResponse.Task> = mutableMapOf()

    override fun handle(message: Message): Message {
        val responsePayload = mapOf("tasks" to taskStats)
        return message.toResponse(payload = responsePayload)
    }
}

class MemoryPoolStatsHandler : Handler {

    override val group = Group.System
    override val command = Command.System.MemoryPoolStats
    override val accept = readOnly

    var memoryPoolStats: MutableMap<String, MemoryPoolStatsResponse.MemoryPool> = mutableMapOf()

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

/*
 * Image Handlers
 */

class ImageWriteHandler : Handler {

    override val group = Group.Image
    override val command = Command.Image.Upload
    override val accept = writeOnly

    var imageData = ByteArray(0)

    override fun handle(message: Message): Message {
        val payload = message.payloadMap
        val data: ByteArray = payload.getNotNull("data")
        val off: Int = payload.getNotNull("off")
        synchronized(this) {
            if (off == 0) {
                val len: Int = payload.getNotNull("len")
                val sha: ByteArray? = payload.getOrNull("sha")
                imageData = ByteArray(len)
            }
            data.copyInto(imageData, off)
        }

        val responsePayload = mapOf("off" to off + data.size)
        return message.toResponse(payload = responsePayload)
    }
}

class CoreReadHandler : Handler {

    override val group = Group.Image
    override val command = Command.Image.CoreDownload
    override val accept = readOnly

    var chunkSize: Int = 512
    var coreData: ByteArray? = null

    override fun handle(message: Message): Message {
        val core = coreData ?: return message.toResponse(ResponseCode.NoEntry)
        val payload = message.payloadMap
        val off: Int = payload.getNotNull("off")

        val size = min(chunkSize, core.size - off)
        val responsePayload = mutableMapOf(
            "off" to off,
            "data" to core.copyOfRange(off, off + size)
        )
        if (off == 0) {
            responsePayload["len"] = core.size
        }
        return message.toResponse(payload = responsePayload)
    }
}

/*
 * File Handlers
 */

class FileWriteHandler : Handler {

    override val group = Group.File
    override val command = Command.File.File
    override val accept = writeOnly

    val files: MutableMap<String, ByteArray> = mutableMapOf()

    override fun handle(message: Message): Message {
        val payload = message.payloadMap
        val fileName: String = payload.getNotNull("name")
        val data: ByteArray = payload.getNotNull("data")
        val off: Int = payload.getNotNull("off")
        synchronized(this) {
            if (off == 0) {
                val len: Int = payload.getNotNull("len")
                files[fileName] = ByteArray(len)
            }
            val file = files[fileName] ?: return message.toResponse(ResponseCode.NoEntry)
            data.copyInto(file, off)
        }

        val responsePayload = mapOf("off" to off + data.size)
        return message.toResponse(payload = responsePayload)
    }
}

class FileReadHandler : Handler {

    override val group = Group.File
    override val command = Command.File.File
    override val accept = readOnly

    var chunkSize: Int = 512
    val files: MutableMap<String, ByteArray> = mutableMapOf()

    override fun handle(message: Message): Message {
        val payload = message.payloadMap
        val off: Int = payload.getNotNull("off")
        val fileName: String = payload.getNotNull("name")
        val file = files[fileName] ?: return message.toResponse(ResponseCode.NoEntry)

        val size = min(chunkSize, file.size - off)
        val responsePayload = mutableMapOf(
            "off" to off,
            "data" to file.copyOfRange(off, off + size)
        )
        if (off == 0) {
            responsePayload["len"] = file.size
        }
        return message.toResponse(payload = responsePayload)
    }
}

/*
 * Utilities
 */

inline fun <reified T> Map<String, Any>.getNotNull(key: String): T {
    val field = checkNotNull(get(key)) { "$key cannot be null" }
    return field as T
}

inline fun <reified T> Map<String, Any>.getOrNull(key: String): T? {
    val field = get(key) ?: return null
    return field as T
}
