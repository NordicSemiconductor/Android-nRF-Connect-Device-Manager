package com.juul.mcumgr.mock.server

import com.juul.mcumgr.message.Format
import com.juul.mcumgr.message.Response.Code
import com.juul.mcumgr.serialization.Message
import com.juul.mcumgr.serialization.decode
import com.juul.mcumgr.serialization.encode
import kotlinx.coroutines.delay

class Server(
    val mtu: Int,
    val format: Format,
    val overrides: MutableList<Handler> = mutableListOf(),
    val delay: Long = 0
) {

    val handlers: List<Handler> = defaultHandlers

    suspend fun handle(requestData: ByteArray): ByteArray {

        if (requestData.size > mtu) {
            error("request data is larger than mtu $mtu")
        }

        val message = requestData.decode(format)

        // First check the override then default handlers. If none exist, return NotSupported error
        val handler = overrides.findHandler(message) ?: handlers.findHandler(message)
            ?: return message.toResponse(Code.NotSupported).encode(format)

        val response = handler.handle(message)
        delay(delay)
        return response.encode(format)
    }

    private fun List<Handler>.findHandler(message: Message): Handler? {
        return find { handler ->
            message.header.group == handler.group.value &&
                message.header.command == handler.command.value &&
                handler.supportedOperations.map { it.value }.contains(message.header.operation)
        }
    }
}
