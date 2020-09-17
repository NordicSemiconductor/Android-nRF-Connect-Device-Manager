import com.juul.mcumgr.Protocol
import com.juul.mcumgr.SystemManager
import com.juul.mcumgr.command.System
import java.util.Date
import java.util.TimeZone
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking
import mock.MockTransport
import mock.server.handler.ConsoleEchoControlHandler
import mock.server.handler.MemoryPoolStatsHandler
import mock.server.handler.ReadDatetimeHandler
import mock.server.Server
import mock.server.handler.TaskStatsHandler
import mock.server.handler.WriteDatetimeHandler
import org.junit.Test
import util.getOrAssert

class SystemManagerTest(protocol: Protocol) : ProtocolParameterizedTest(protocol) {

    private val mtu = 512
    private val server = Server(mtu, protocol)
    private val transport = MockTransport(mtu, protocol, server)
    private val manager = SystemManager(transport)

    @Test
    fun `echo, success`() = runBlocking {
        val echo = "Hello McuManager!"
        val response = manager.echo(echo).getOrAssert()
        assertEquals(echo, response.echo)
    }

    @Test
    fun `console echo control, success`() = runBlocking {
        val enabled = true
        val handler: ConsoleEchoControlHandler = server.findHandler()
        manager.consoleEchoControl(enabled).getOrAssert()
        assertEquals(enabled, handler.enabled)
    }

    @Test
    fun `task stats, success`() = runBlocking {
        val handler: TaskStatsHandler = server.findHandler()
        handler.taskStats = mutableMapOf(
            "my_task" to System.TaskStatsResponse.Task(1, 2, 3, 4, 5, 6, 7, 8, 9)
        )
        val response = manager.taskStats().getOrAssert()
        assertEquals(handler.taskStats.keys, response.tasks.keys)
        handler.taskStats.forEach { (name, handlerStats) ->
            val responseStats = response.tasks[name]
            assertNotNull(responseStats) { "expected task $name not found in response" }
            assertEquals(handlerStats.priority, responseStats.priority)
            assertEquals(handlerStats.taskId, responseStats.taskId)
            assertEquals(handlerStats.state, responseStats.state)
            assertEquals(handlerStats.stackUse, responseStats.stackUse)
            assertEquals(handlerStats.stackSize, responseStats.stackSize)
            assertEquals(handlerStats.contextSwitchCount, responseStats.contextSwitchCount)
            assertEquals(handlerStats.runtime, responseStats.runtime)
            assertEquals(handlerStats.lastCheckIn, responseStats.lastCheckIn)
            assertEquals(handlerStats.nextCheckIn, responseStats.nextCheckIn)
        }
    }

    @Test
    fun `memory pool stats, success`() = runBlocking {
        val handler: MemoryPoolStatsHandler = server.findHandler()
        handler.memoryPoolStats = mutableMapOf(
            "memory_pool" to System.MemoryPoolStatsResponse.MemoryPool(1, 2, 3, 4)
        )
        val response = manager.memoryPoolStats().getOrAssert()
        assertEquals(handler.memoryPoolStats.keys, response.memoryPools.keys)
        handler.memoryPoolStats.forEach { (name, handlerPool) ->
            val responsePool = response.memoryPools[name]
            assertNotNull(responsePool) { "expected memory pool $name not found in response" }
            assertEquals(handlerPool.blockSize, responsePool.blockSize)
            assertEquals(handlerPool.blocks, responsePool.blocks)
            assertEquals(handlerPool.freeBlocks, responsePool.freeBlocks)
        }
    }

    @Test
    fun `read datetime, success`() = runBlocking {
        val now = Date()
        val handler: ReadDatetimeHandler = server.findHandler()
        handler.date = now
        val response = manager.readDatetime().getOrAssert()
        assertEquals(handler.date, response.date)
    }

    @Test
    fun `write datetime, success`() = runBlocking {
        val notNow = Date(0)
        val now = Date()
        val handler: WriteDatetimeHandler = server.findHandler()
        handler.date = notNow
        manager.writeDatetime(now, TimeZone.getTimeZone("UTC")).getOrAssert()
        assertEquals(now, handler.date)
    }

    @Test
    fun `reset, success`() = runBlocking {
        manager.reset().getOrAssert()
    }
}
