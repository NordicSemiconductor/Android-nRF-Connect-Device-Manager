package com.juul.mcumgr.mock

import com.juul.mcumgr.Request
import com.juul.mcumgr.Response
import com.juul.mcumgr.Transport
import com.juul.mcumgr.mock.server.Server
import com.juul.mcumgr.serialization.Header
import com.juul.mcumgr.serialization.Message
import com.juul.mcumgr.serialization.decodeCoap
import com.juul.mcumgr.serialization.decodeStandard
import com.juul.mcumgr.serialization.encodeCoap
import com.juul.mcumgr.serialization.encodeStandard

class MockTransport(
    override val mtu: Int,
    override val scheme: Transport.Scheme,
    val server: Server
) : Transport {

    override suspend fun <T : Response> send(request: Request, responseType: Class<T>): T {
        val requestData = request.encode(scheme)
        val responseData = server.handle(requestData)
        return responseData.decode(scheme, responseType)
    }
}

private fun <T : Request> T.encode(scheme: Transport.Scheme): ByteArray {
    val header = Header(operation.value, group.value, command.value, 0,
        SequenceNumber.next, 0)
    val message = Message(header, this)
    return when (scheme) {
        Transport.Scheme.STANDARD -> message.encodeStandard()
        Transport.Scheme.COAP -> message.encodeCoap()
    }
}

private fun <T : Response> ByteArray.decode(scheme: Transport.Scheme, type: Class<T>): T {
    val message: Message<T> = when (scheme) {
        Transport.Scheme.STANDARD -> decodeStandard(type)
        Transport.Scheme.COAP -> decodeCoap(type)
    }
    return message.payload
}

object SequenceNumber {
    private var n = 0
    val next = synchronized(this) {
        n = if (n == 255) {
            0
        } else {
            n + 1
        }
        n
    }
}
