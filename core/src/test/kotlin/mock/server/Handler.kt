package com.juul.mcumgr.mock.server

import com.juul.mcumgr.Command
import com.juul.mcumgr.Group
import com.juul.mcumgr.Operation

val readOnly = setOf(Operation.Read)
val writeOnly = setOf(Operation.Write)
val readWrite = setOf(Operation.Read, Operation.Write)

interface Handler {

    val group: Group
    val command: Command
    val supportedOperations: Set<Operation>

    fun handle(message: ServerMessage): ServerMessage
}

val defaultHandlers = listOf<Handler>(
    EchoHandler()
)

class EchoHandler : Handler {

    override val group = Group.System
    override val command = Command.System.Echo
    override val supportedOperations = readWrite

    override fun handle(message: ServerMessage): ServerMessage {
        val echo = message.payload["d"] ?: error("echo data must not be null")
        val responsePayload = mapOf("r" to echo)
        return message.toResponse(payload = responsePayload)
    }
}
