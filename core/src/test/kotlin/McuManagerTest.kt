import com.juul.mcumgr.McuManager
import com.juul.mcumgr.SendResult
import com.juul.mcumgr.SendResult.Failure
import com.juul.mcumgr.getOrThrow
import com.juul.mcumgr.message.Protocol
import com.juul.mcumgr.message.ResponseCode
import com.juul.mcumgr.message.TaskStatsResponse
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

class McuManagerTest(protocol: Protocol) : ProtocolParameterizedTest(protocol) {

    private val mtu = 512
    private val server = Server(mtu, protocol)
    private val transport = MockTransport(mtu, protocol, server)
    private val mcuManager = McuManager(transport)

    @Test
    fun `error code response result expected`() = runBlocking {
        server.overrides.add(EchoHandler().toErrorResponseHandler(ResponseCode.BadState))
        when (val result = mcuManager.system.echo("test")) {
            is SendResult.Response -> assertEquals(ResponseCode.BadState, result.code)
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

    // System

    @Test
    fun `echo success`() = runBlocking {
        val echo = "Hello McuManager!"
        val response = mcuManager.system.echo(echo).getOrThrow()
        assertEquals(echo, response.echo)
    }

    @Test
    fun `task stats success`() = runBlocking {
        val handler: TaskStatsHandler = server.findHandler()
        handler.taskStats = mutableMapOf(
            "my_task" to TaskStatsResponse.Task(1, 2, 3, 4, 5, 6, 7, 8, 9)
        )
        val response = mcuManager.system.taskStats().getOrThrow()
        assertEquals(response.tasks, handler.taskStats)
    }

    // Image

    @Test
    fun `image write success`() = runBlocking {
        val imageData = Random.Default.nextBytes(200)

        val firstChunk = imageData.copyOfRange(0, 100)
        var response = mcuManager.image.imageWrite(
            firstChunk,
            0,
            200
        ).getOrThrow()
        assertEquals(100, response.offset)

        val secondChunk = imageData.copyOfRange(100, imageData.size)
        response = mcuManager.image.imageWrite(
            secondChunk,
            100
        ).getOrThrow()
        assertEquals(200, response.offset)
        assertByteArrayEquals(imageData, server.getImageUploadData())
    }

    // Files

    @Test
    fun `file write success`() = runBlocking {
        val fileData = Random.Default.nextBytes(200)

        val firstChunk = fileData.copyOfRange(0, 100)
        var response = mcuManager.files.fileWrite(
            "my_file",
            firstChunk,
            0,
            200
        ).getOrThrow()
        assertEquals(100, response.offset)

        val secondChunk = fileData.copyOfRange(100, fileData.size)
        response = mcuManager.files.fileWrite(
            "my_file",
            secondChunk,
            100
        ).getOrThrow()
        assertEquals(200, response.offset)
        assertByteArrayEquals(
            fileData,
            server.getFileUploadData("my_file")
        )
    }
}
