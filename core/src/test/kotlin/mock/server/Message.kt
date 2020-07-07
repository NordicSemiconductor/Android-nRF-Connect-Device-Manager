package com.juul.mcumgr.mock.server

import com.juul.mcumgr.Operation
import com.juul.mcumgr.Response
import com.juul.mcumgr.serialization.Header
import com.juul.mcumgr.serialization.Message

/**
 * Core API was designed solely around client side functionality. Therefore responses are not
 * expected to be sent/encoded and requests are not expected to be receive/decoded.
 *
 * Using the serialization module's message with a map rather than an specific object payload
 * gives us greater flexibility for a server implementation without polluting the core.
 */
typealias ServerMessage = Message<Map<String, Any>>

fun ServerMessage.toResponse(
    code: Response.Code = Response.Code.Ok,
    payload: Map<String, Any> = emptyMap()
): ServerMessage {
    val responseHeader = header.toResponse()
    val responsePayload = payload.toMutableMap().apply {
        this["rc"] = code.value
    }
    return Message(responseHeader, responsePayload)
}

fun Header.toResponse(): Header {
    val newOperation = when (operation) {
        Operation.Read.value -> Operation.ReadResponse.value
        Operation.Write.value -> Operation.WriteResponse.value
        else -> operation
    }
    return copy(operation = newOperation)
}
