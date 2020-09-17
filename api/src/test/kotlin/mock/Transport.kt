package mock

import com.juul.mcumgr.CommandResult
import com.juul.mcumgr.Protocol
import com.juul.mcumgr.Request
import com.juul.mcumgr.Response
import com.juul.mcumgr.Transport
import com.juul.mcumgr.serialization.decode
import com.juul.mcumgr.serialization.encode
import mock.server.Server
import kotlin.reflect.KClass

class MockTransport(
    override val mtu: Int,
    override val protocol: Protocol,
    private val server: Server
) : Transport {

    override suspend fun <T : Response> send(
        request: Request,
        responseType: KClass<T>
    ): CommandResult<T> {
        val requestData = request.encode(protocol, SequenceNumber.next)
        val responseData = try {
             server.handle(requestData)
        } catch (e: Throwable) {
            return CommandResult.Failure(e)
        }
        return responseData.decode(protocol, responseType.java)
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
