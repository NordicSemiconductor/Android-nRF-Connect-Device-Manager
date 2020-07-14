package mock.server

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.node.ObjectNode
import com.juul.mcumgr.message.Operation
import com.juul.mcumgr.message.Response
import com.juul.mcumgr.serialization.Header
import com.juul.mcumgr.serialization.Message
import com.juul.mcumgr.serialization.cbor

fun Message.toResponse(
    code: Response.Code = Response.Code.Ok,
    payload: Map<String, Any> = emptyMap()
): Message {
    val responseHeader = header.toResponse()
    val responsePayload = payload.toMutableMap().apply {
        this["rc"] = code.value
    }
    return Message(responseHeader, cbor.valueToTree(responsePayload))
}

fun Header.toResponse(): Header {
    val newOperation = when (operation) {
        Operation.Read.value -> Operation.ReadResponse.value
        Operation.Write.value -> Operation.WriteResponse.value
        else -> operation
    }
    return copy(operation = newOperation)
}

val Message.payloadMap: Map<String, Any> get() = payload.toMap()

fun ObjectNode.toMap(): Map<String, Any> =
    cbor.convertValue(this, object : TypeReference<Map<String, Any>>() {})
