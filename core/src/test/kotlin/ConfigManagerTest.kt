import com.juul.mcumgr.ConfigManager
import com.juul.mcumgr.Protocol
import com.juul.mcumgr.command.ResponseCode
import kotlinx.coroutines.runBlocking
import mock.MockTransport
import mock.server.Server
import mock.server.handler.ConfigHandler
import org.junit.After
import org.junit.Test
import util.assertResponseCode
import util.getOrAssert
import kotlin.test.assertEquals

class ConfigManagerTest(protocol: Protocol) : ProtocolParameterizedTest(protocol) {

    private val mtu = 512
    private val server = Server(mtu, protocol)
    private val transport = MockTransport(mtu, protocol, server)
    private val manager = ConfigManager(transport)

    @After
    fun `reset server`() {
        server.reset()
    }

    @Test
    fun `config read, success`() = runBlocking {
        val handler: ConfigHandler = server.findHandler()
        val configKey = "key"
        val configValue = "val"
        handler.config = mutableMapOf(
            configKey to configValue
        )
        manager.read("asdf").assertResponseCode(ResponseCode.InValue)
        val response = manager.read(configKey).getOrAssert()
        assertEquals(configValue, response.value)
    }

    @Test
    fun `config write, success`() = runBlocking {
        val handler: ConfigHandler = server.findHandler()
        val configKey = "key"
        val configValue = "val"
        val newConfigValue = "new"
        handler.config = mutableMapOf(
            configKey to configValue
        )
        manager.write("asdf", "asdf").assertResponseCode(ResponseCode.InValue)
        manager.write(configKey, newConfigValue).getOrAssert()
        assertEquals(newConfigValue, handler.config[configKey])
    }
}
