import com.juul.mcumgr.CommandResult
import com.juul.mcumgr.CommandResult.Failure
import com.juul.mcumgr.McuManager
import com.juul.mcumgr.Protocol
import com.juul.mcumgr.ResponseCode
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import mock.MockTransport
import mock.server.handler.EchoHandler
import mock.server.Server
import mock.server.handler.toErrorResponseHandler
import mock.server.handler.toThrowHandler
import org.junit.Test
import util.ExpectedException
import util.assertResponseCode

class McuManagerTest(protocol: Protocol) : ProtocolParameterizedTest(protocol) {

    private val mtu = 512
    private val server = Server(mtu, protocol)
    private val transport = MockTransport(mtu, protocol, server)
    private val mcuManager = McuManager(transport)

    @Test
    fun `error code response result expected`() = runBlocking {
        val code = ResponseCode.BadState
        server.overrides.add(EchoHandler().toErrorResponseHandler(code))
        mcuManager.system.echo("test").assertResponseCode(code)
        Unit
    }

    @Test
    fun `failure result expected`() = runBlocking {
        server.overrides.add(EchoHandler().toThrowHandler(ExpectedException))
        when (val result = mcuManager.system.echo("test")) {
            is Failure -> assertEquals(ExpectedException, result.throwable)
            is CommandResult.Response -> error("expected failure exception, got $result")
        }
    }
}
