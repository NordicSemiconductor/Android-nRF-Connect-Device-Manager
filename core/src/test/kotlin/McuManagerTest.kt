import com.juul.mcumgr.McuManager
import com.juul.mcumgr.SendResult
import com.juul.mcumgr.SendResult.Failure
import com.juul.mcumgr.getOrThrow
import com.juul.mcumgr.command.Protocol
import com.juul.mcumgr.command.ResponseCode
import com.juul.mcumgr.command.TaskStatsResponse
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import mock.MockTransport
import mock.server.EchoHandler
import mock.server.Server
import mock.server.TaskStatsHandler
import mock.server.toErrorResponseHandler
import mock.server.toThrowHandler
import org.junit.Test
import utils.ExpectedException
import utils.assertByteArrayEquals
import kotlin.test.assertNull

class McuManagerTest(protocol: Protocol) : ProtocolParameterizedTest(protocol) {

    private val mtu = 512
    private val server = Server(mtu, protocol)
    private val transport = MockTransport(mtu, protocol, server)
    private val mcuManager = McuManager(transport)

    @Test
    fun `error code response result expected`() = runBlocking {
        server.overrides.add(EchoHandler().toErrorResponseHandler(ResponseCode.BadState))
        when (val result = mcuManager.system.echo("test")) {
            is SendResult.Response -> {
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
            is SendResult.Response -> error("expected failure exception, got $result")
        }
    }
}
