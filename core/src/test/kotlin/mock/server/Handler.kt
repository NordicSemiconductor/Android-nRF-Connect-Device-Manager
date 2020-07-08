package com.juul.mcumgr.mock.server

import com.juul.mcumgr.message.Command
import com.juul.mcumgr.message.Group
import com.juul.mcumgr.message.Operation
import com.juul.mcumgr.message.Response
import com.juul.mcumgr.serialization.Message

val readOnly = setOf(Operation.Read)
val writeOnly = setOf(Operation.Write)
val readWrite = setOf(Operation.Read, Operation.Write)

interface Handler {

    val group: Group
    val command: Command
    val supportedOperations: Set<Operation>

    fun handle(message: Message): Message
}

fun Handler.toErrorResponseHandler(code: Response.Code): ErrorResponseHandler {
    return ErrorResponseHandler(code, group, command)
}

fun Handler.toThrowHandler(throwable: Throwable): ThrowHandler {
    return ThrowHandler(throwable, group, command)
}

val defaultHandlers = listOf<Handler>(
    EchoHandler()
)

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

class EchoHandler : Handler {

    override val group = Group.System
    override val command = Command.System.Echo
    override val supportedOperations = readWrite

    override fun handle(message: Message): Message {
        val payload = message.payloadMap
        val echo = payload["d"] ?: error("echo data must not be null")
        val responsePayload = mapOf("r" to echo)
        return message.toResponse(payload = responsePayload)
    }
}
