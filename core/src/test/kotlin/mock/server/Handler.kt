package mock.server

import com.juul.mcumgr.message.Command
import com.juul.mcumgr.message.Group
import com.juul.mcumgr.message.Operation
import com.juul.mcumgr.message.Response
import com.juul.mcumgr.serialization.Message
import kotlin.math.min

val readOnly = setOf(Operation.Read)
val writeOnly = setOf(Operation.Write)
val readWrite = setOf(Operation.Read, Operation.Write)

/**
 * Handles a request with a specific group, command, and operation.
 */
interface Handler {

    val group: Group
    val command: Command
    val supportedOperations: Set<Operation>

    fun handle(message: Message): Message
}

val defaultHandlers = listOf<Handler>(
    EchoHandler(),
    ImageWriteHandler(),
    CoreReadHandler(),
    FileWriteHandler(),
    FileReadHandler()
)

/*
 * Error Handlers
 */

fun Handler.toErrorResponseHandler(code: Response.Code): ErrorResponseHandler {
    return ErrorResponseHandler(code, group, command)
}

fun Handler.toThrowHandler(throwable: Throwable): ThrowHandler {
    return ThrowHandler(throwable, group, command)
}

class ErrorResponseHandler(
    val code: Response.Code,
    override val group: Group,
    override val command: Command
) : Handler {
    override val supportedOperations = readWrite

    override fun handle(message: Message): Message {
        return message.toResponse(code)
    }
}

class ThrowHandler(
    val throwable: Throwable,
    override val group: Group,
    override val command: Command
) : Handler {

    override val supportedOperations = readWrite

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
    override val supportedOperations = readWrite

    override fun handle(message: Message): Message {
        val payload = message.payloadMap
        val echo: String = payload.getNotNull("d")
        val responsePayload = mapOf("r" to echo)
        return message.toResponse(payload = responsePayload)
    }
}

/*
 * Image Handlers
 */

class ImageWriteHandler : Handler {

    override val group = Group.Image
    override val command = Command.Image.Upload
    override val supportedOperations = writeOnly

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
    override val command = Command.Image.CoreLoad
    override val supportedOperations = readOnly

    var chunkSize: Int = 512
    var coreData: ByteArray? = null

    override fun handle(message: Message): Message {
        val core = coreData ?: return message.toResponse(Response.Code.NoEntry)
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

    override val group = Group.Files
    override val command = Command.Files.File
    override val supportedOperations = writeOnly

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
            val file = files[fileName] ?: return message.toResponse(Response.Code.NoEntry)
            data.copyInto(file, off)
        }

        val responsePayload = mapOf("off" to off + data.size)
        return message.toResponse(payload = responsePayload)
    }
}

class FileReadHandler : Handler {

    override val group = Group.Files
    override val command = Command.Files.File
    override val supportedOperations = readOnly

    var chunkSize: Int = 512
    val files: MutableMap<String, ByteArray> = mutableMapOf()

    override fun handle(message: Message): Message {
        val payload = message.payloadMap
        val off: Int = payload.getNotNull("off")
        val fileName: String = payload.getNotNull("name")
        val file = files[fileName] ?: return message.toResponse(Response.Code.NoEntry)

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
