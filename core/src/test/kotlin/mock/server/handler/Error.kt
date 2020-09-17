package mock.server.handler

import com.juul.mcumgr.command.Command
import com.juul.mcumgr.command.Group
import com.juul.mcumgr.command.ResponseCode
import com.juul.mcumgr.serialization.Message
import mock.server.toResponse

fun Handler.toErrorResponseHandler(code: ResponseCode): ErrorResponseHandler {
    return ErrorResponseHandler(code, group, command)
}

fun Handler.toThrowHandler(throwable: Throwable): ThrowHandler {
    return ThrowHandler(throwable, group, command)
}

class ErrorResponseHandler(
    private val code: ResponseCode,
    override val group: Group,
    override val command: Command
) : Handler {
    override val accept = readWrite

    override fun handle(message: Message): Message {
        return message.toResponse(code)
    }
}

class ThrowHandler(
    private val throwable: Throwable,
    override val group: Group,
    override val command: Command
) : Handler {

    override val accept = readWrite

    override fun handle(message: Message): Message {
        throw throwable
    }
}
