import com.juul.mcumgr.CommandResult
import com.juul.mcumgr.CommandResult.Failure
import com.juul.mcumgr.McuManager
import com.juul.mcumgr.Protocol
import com.juul.mcumgr.ResponseCode
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import mock.MockTransport
import mock.server.EchoHandler
import mock.server.Server
import mock.server.toErrorResponseHandler
import mock.server.toThrowHandler
import org.junit.Test
import utils.ExpectedException

class McuManagerTest(protocol: Protocol) : ProtocolParameterizedTest(protocol) {

    private val mtu = 512
    private val server = Server(mtu, protocol)
    private val transport = MockTransport(mtu, protocol, server)
    private val mcuManager = McuManager(transport)

    @Test
    fun `error code response result expected`() = runBlocking {
        server.overrides.add(EchoHandler().toErrorResponseHandler(ResponseCode.BadState))
        when (val result = mcuManager.system.echo("test")) {
            is CommandResult.Response -> {
                assertEquals(ResponseCode.BadState, result.code)
                assertNull(result.body)
            }
            is Failure -> error("expected McuMgrResult.Response, got $result")
        }
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
