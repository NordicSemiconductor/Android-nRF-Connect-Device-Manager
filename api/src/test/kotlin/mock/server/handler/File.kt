package mock.server.handler

import com.juul.mcumgr.Command
import com.juul.mcumgr.Group
import com.juul.mcumgr.ResponseCode
import com.juul.mcumgr.serialization.Message
import mock.server.payloadMap
import mock.server.toResponse
import util.getNotNull
import kotlin.math.min

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
