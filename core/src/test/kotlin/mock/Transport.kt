package com.juul.mcumgr.mock

import com.juul.mcumgr.McuMgrResult
import com.juul.mcumgr.Transport
import com.juul.mcumgr.message.Format
import com.juul.mcumgr.message.Request
import com.juul.mcumgr.message.Response
import com.juul.mcumgr.mock.server.Server
import com.juul.mcumgr.serialization.decode
import com.juul.mcumgr.serialization.encode

class MockTransport(
    override val mtu: Int,
    override val format: Format,
    val server: Server
) : Transport {

    override suspend fun <T : Response> send(request: Request, responseType: Class<T>): McuMgrResult<T> {
        val requestData = request.encode(format)
        val responseData = server.handle(requestData)
        return responseData.decode(format, responseType)
    }
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
