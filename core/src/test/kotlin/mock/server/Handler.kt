package mock.server

import com.juul.mcumgr.message.Command
import com.juul.mcumgr.message.Group
import com.juul.mcumgr.message.Operation
import com.juul.mcumgr.message.Response
import com.juul.mcumgr.serialization.Message
import okio.Buffer

val readOnly = setOf(Operation.Read)
val writeOnly = setOf(Operation.Write)
val readWrite = setOf(Operation.Read, Operation.Write)

interface Handler {

    val group: Group
    val command: Command
    val supportedOperations: Set<Operation>

    fun handle(message: Message): Message
}

val defaultHandlers = listOf<Handler>(
    EchoHandler(),
    ImageWriteHandler(),
    FileWriteHandler()
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
                val sha: ByteArray = payload.getNotNull("sha")
                imageData = ByteArray(len)
            }
            data.copyInto(imageData, off)
        }

        val responsePayload = mapOf("off" to off + data.size)
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

    var fileData = ByteArray(0)

    override fun handle(message: Message): Message {
        val payload = message.payloadMap
        val fileName: String = payload.getNotNull("name")
        val data: ByteArray = payload.getNotNull("data")
        val off: Int = payload.getNotNull("off")
        synchronized(this) {
            if (off == 0) {
                val len: Int = payload.getNotNull("len")
                fileData = ByteArray(len)
            }
            data.copyInto(fileData, off)
        }

        val responsePayload = mapOf("off" to off + data.size)
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

inline fun <reified T> Map<String, Any>.getOrNull(key: String): T {
    return get(key) as T
}

private fun ByteArray.copyTo(buffer: Buffer, offset: Int) {
    Buffer().write(this).copyTo(buffer, offset.toLong())
}
