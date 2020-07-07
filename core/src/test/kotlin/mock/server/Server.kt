package com.juul.mcumgr.mock.server

import com.fasterxml.jackson.core.type.TypeReference
import com.juul.mcumgr.Response.Code
import com.juul.mcumgr.Transport
import com.juul.mcumgr.serialization.decodeCoap
import com.juul.mcumgr.serialization.decodeStandard
import com.juul.mcumgr.serialization.encodeCoap
import com.juul.mcumgr.serialization.encodeStandard

class Server(
    val mtu: Int,
    val scheme: Transport.Scheme,
    val overrides: List<Handler> = listOf(),
    val delay: Long = 0
) {

    val handlers: List<Handler> = defaultHandlers

    fun handle(requestData: ByteArray): ByteArray {

        val message = requestData.decode(scheme)

        // First check the override then default handlers. If none exist, return NotSupported error
        val handler = overrides.findHandler(message) ?: handlers.findHandler(message) ?:
            return message.toResponse(Code.NotSupported).encode(scheme)

        val response = handler.handle(message)
        return response.encode(scheme)
    }

    private fun List<Handler>.findHandler(message: ServerMessage): Handler? {
        return find { handler ->
            message.header.group == handler.group.value &&
                message.header.command == handler.command.value &&
                handler.supportedOperations.map { it.value }.contains(message.header.operation)
        }
    }
}

private fun ByteArray.decode(scheme: Transport.Scheme): ServerMessage {
    return when (scheme) {
        Transport.Scheme.STANDARD -> decodeStandard(object : TypeReference<Map<String, Any>>() {})
        Transport.Scheme.COAP -> decodeCoap(object : TypeReference<Map<String, Any>>() {})
    }
}

private fun ServerMessage.encode(scheme: Transport.Scheme): ByteArray {
    return when(scheme) {
        Transport.Scheme.STANDARD -> encodeStandard()
        Transport.Scheme.COAP -> encodeCoap()
    }
}



