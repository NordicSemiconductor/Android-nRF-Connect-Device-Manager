package mock.server.handler

import com.juul.mcumgr.command.Command
import com.juul.mcumgr.command.Group
import com.juul.mcumgr.command.Operation
import com.juul.mcumgr.command.ResponseCode
import com.juul.mcumgr.command.Image
import com.juul.mcumgr.serialization.Message
import mock.server.payloadMap
import mock.server.toResponse
import util.getNotNull
import util.getOrNull
import kotlin.math.min

class ImageStateHandler : Handler {

    override val group = Group.Image
    override val command = Command.Image.State
    override val accept = readWrite

    internal var images: MutableList<Image.StateResponse.ImageState> = mutableListOf()

    override fun handle(message: Message): Message {
        return when (message.header.operation) {
            Operation.Read.value -> handleRead(message)
            Operation.Write.value -> handleWrite(message)
            else -> error("can only handle requests")
        }
    }

    private fun handleRead(message: Message): Message {
        val responsePayload: Map<String, Any> = mapOf("images" to images)
        return message.toResponse(payload = responsePayload)
    }

    private fun handleWrite(message: Message): Message {
        val payload = message.payloadMap
        val confirm: Boolean = payload.getNotNull("confirm")
        val hash: ByteArray? = payload.getOrNull("hash")

        val image = images.find {
            hash?.contentEquals(it.hash) ?: false
        } ?: images[0]

        // Note: This is incorrect compared to the actual bootloader.
        // TODO Better replicate bootloader behavior
        if (confirm) {
            images[image.slot] = image.copy(confirmed = true)
        } else {
            images[image.slot] = image.copy(pending = true)
        }

        return handleRead(message)
    }
}

class ImageEraseHandler : Handler {

    override val group = Group.Image
    override val command = Command.Image.Erase
    override val accept = writeOnly

    var imageData: ByteArray? = null

    override fun handle(message: Message): Message {
        imageData = null
        return message.toResponse()
    }
}

class ImageEraseStateHandler : Handler {

    override val group = Group.Image
    override val command = Command.Image.EraseState
    override val accept = writeOnly

    var erased = false

    override fun handle(message: Message): Message {
        erased = true
        return message.toResponse()
    }
}

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

class CoreListHandler : Handler {

    override val group = Group.Image
    override val command = Command.Image.CoreList
    override val accept = readOnly

    var coreData: ByteArray? = null

    override fun handle(message: Message): Message {
        return if (coreData == null) {
            message.toResponse(ResponseCode.NoEntry)
        } else {
            message.toResponse()
        }
    }
}

class CoreReadHandler : Handler {

    override val group = Group.Image
    override val command = Command.Image.CoreLoad
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

class CoreEraseHandler : Handler {

    override val group = Group.Image
    override val command = Command.Image.CoreLoad
    override val accept = writeOnly

    var coreData: ByteArray? = null

    override fun handle(message: Message): Message {
        coreData = null
        return message.toResponse()
    }
}
