import com.juul.mcumgr.SystemManager
import com.juul.mcumgr.command.MemoryPoolStatsResponse
import com.juul.mcumgr.command.Protocol
import com.juul.mcumgr.command.TaskStatsResponse
import com.juul.mcumgr.getOrThrow
import java.util.Date
import java.util.TimeZone
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import mock.MockTransport
import mock.server.ConsoleEchoControlHandler
import mock.server.MemoryPoolStatsHandler
import mock.server.ReadDatetimeHandler
import mock.server.Server
import mock.server.TaskStatsHandler
import mock.server.WriteDatetimeHandler
import org.junit.Test

class SystemManagerTest(protocol: Protocol) : ProtocolParameterizedTest(protocol) {

    private val mtu = 512
    private val server = Server(mtu, protocol)
    private val transport = MockTransport(mtu, protocol, server)
    private val manager = SystemManager(transport)

    @Test
    fun `echo, success`() = runBlocking {
        val echo = "Hello McuManager!"
        val response = manager.echo(echo).getOrThrow()
        assertEquals(echo, response.echo)
    }

    @Test
    fun `console echo control, success`() = runBlocking {
        val enabled = true
        val handler: ConsoleEchoControlHandler = server.findHandler()
        manager.consoleEchoControl(enabled).getOrThrow()
        assertEquals(enabled, handler.enabled)
    }

    @Test
    fun `task stats, success`() = runBlocking {
        val handler: TaskStatsHandler = server.findHandler()
        handler.taskStats = mutableMapOf(
            "my_task" to TaskStatsResponse.Task(1, 2, 3, 4, 5, 6, 7, 8, 9)
        )
        val response = manager.taskStats().getOrThrow()
        assertEquals(handler.taskStats, response.tasks)
    }

    @Test
    fun `memory pool stats, success`() = runBlocking {
        val handler: MemoryPoolStatsHandler = server.findHandler()
        handler.memoryPoolStats = mutableMapOf(
            "memory_pool" to MemoryPoolStatsResponse.MemoryPool(1, 2, 3, 4)
        )
        val response = manager.memoryPoolStats().getOrThrow()
        assertEquals(handler.memoryPoolStats, response.memoryPools)
    }

    @Test
    fun `read datetime, success`() = runBlocking {
        val now = Date()
        val handler: ReadDatetimeHandler = server.findHandler()
        handler.date = now
        val response = manager.readDatetime().getOrThrow()
        assertEquals(handler.date, response.date)
    }

    @Test
    fun `write datetime, success`() = runBlocking {
        val notNow = Date(0)
        val now = Date()
        val handler: WriteDatetimeHandler = server.findHandler()
        handler.date = notNow
        manager.writeDatetime(now, TimeZone.getTimeZone("UTC")).getOrThrow()
        assertEquals(now, handler.date)
    }

    @Test
    fun `reset, success`() = runBlocking {
        manager.reset().getOrThrow()
        Unit
    }
}
