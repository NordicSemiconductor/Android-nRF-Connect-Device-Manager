import com.juul.mcumgr.ConfigManager
import com.juul.mcumgr.Protocol
import com.juul.mcumgr.StatsManager
import com.juul.mcumgr.command.ResponseCode
import kotlinx.coroutines.runBlocking
import mock.MockTransport
import mock.server.Server
import mock.server.handler.ConfigHandler
import mock.server.handler.StatsListHandler
import mock.server.handler.StatsReadHandler
import org.junit.After
import org.junit.Test
import util.assertResponseCode
import util.getOrAssert
import kotlin.test.assertEquals

class StatsManagerTest(protocol: Protocol) : ProtocolParameterizedTest(protocol) {

    private val mtu = 512
    private val server = Server(mtu, protocol)
    private val transport = MockTransport(mtu, protocol, server)
    private val manager = StatsManager(transport)

    private val stats: MutableMap<String, MutableMap<String, Long>> = mutableMapOf(
        "g1" to mutableMapOf(
            "s1" to 1L,
            "s2" to 2L
        ),
        "g2" to mutableMapOf(
            "s1" to 1L,
            "s2" to 2L
        )
    )

    @After
    fun `reset server`() {
        server.reset()
    }

    @Test
    fun `stats read, success`() = runBlocking {
        val handler: StatsReadHandler = server.findHandler()
        handler.stats = stats
        manager.read("asdf").assertResponseCode(ResponseCode.NoEntry)
        val response = manager.read("g1").getOrAssert()
        assertEquals(stats["g1"]!!, response.fields)
    }

    @Test
    fun `stats list, success`() = runBlocking {
        val handler: StatsListHandler = server.findHandler()
        handler.groups = stats.keys.toList()
        val response = manager.list().getOrAssert()
        assertEquals(stats.keys.toList(), response.groups)

    }
}
