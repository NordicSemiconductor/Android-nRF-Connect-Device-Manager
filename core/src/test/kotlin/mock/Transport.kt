package mock

import com.juul.mcumgr.SendResult
import com.juul.mcumgr.Transport
import com.juul.mcumgr.command.Protocol
import com.juul.mcumgr.command.Request
import com.juul.mcumgr.command.Response
import com.juul.mcumgr.serialization.decode
import com.juul.mcumgr.serialization.encode
import mock.server.Server

class MockTransport(
    override val mtu: Int,
    override val protocol: Protocol,
    private val server: Server
) : Transport {

    override suspend fun <T : Response> Transport.send(
        request: Request, responseType: Class<T>
    ): SendResult<T> {
        val requestData = request.encode(protocol, SequenceNumber.next)
        val responseData = try {
             server.handle(requestData)
        } catch (e: Throwable) {
            return SendResult.Failure(e)
        }
        return responseData.decode(protocol, responseType)
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
