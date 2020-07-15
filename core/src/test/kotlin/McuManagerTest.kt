import com.juul.mcumgr.McuManager
import com.juul.mcumgr.McuMgrResult.Error
import com.juul.mcumgr.McuMgrResult.Failure
import com.juul.mcumgr.McuMgrResult.Success
import com.juul.mcumgr.getOrThrow
import com.juul.mcumgr.message.Format
import com.juul.mcumgr.message.Response
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import mock.MockTransport
import mock.server.EchoHandler
import mock.server.Server
import mock.server.toErrorResponseHandler
import mock.server.toThrowHandler
import org.junit.Test
import utils.ExpectedException
import utils.assertByteArrayEquals

class McuManagerTest(format: Format) : FormatParameterizedTest(format) {

    private val mtu = 512
    private val server = Server(mtu, format)
    private val transport = MockTransport(mtu, format, server)
    private val mcuManager = McuManager(transport)

    @Test
    fun `error response result expected`() = runBlocking {
        server.overrides.add(EchoHandler().toErrorResponseHandler(Response.Code.BadState))
        when (val result = mcuManager.echo("test")) {
            is Error -> assertEquals(Response.Code.BadState, result.code)
            is Success, is Failure -> error("expected McuMgrResult.Error, got $result")
        }
    }

    @Test
    fun `failure result expected`() = runBlocking {
        server.overrides.add(EchoHandler().toThrowHandler(ExpectedException))
        when (val result = mcuManager.echo("test")) {
            is Failure -> assertEquals(ExpectedException, result.throwable)
            is Success, is Error -> error("expected failure exception, got $result")
        }
    }

    // System

    @Test
    fun `echo success`() = runBlocking {
        val echo = "Hello McuManager!"
        val response = mcuManager.echo(echo).getOrThrow()
        assertEquals(echo, response.echo)
    }

    // Image

    @Test
    fun `image write success`() = runBlocking {
        val imageData = Random.Default.nextBytes(200)

        val firstChunk = imageData.copyOfRange(0, 100)
        var response = mcuManager.imageWrite(
            firstChunk,
            0,
            200
        ).getOrThrow()
        assertEquals(100, response.offset)

        val secondChunk = imageData.copyOfRange(100, imageData.size)
        response = mcuManager.imageWrite(
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
        var response = mcuManager.fileWrite(
            "my_file",
            firstChunk,
            0,
            200
        ).getOrThrow()
        assertEquals(100, response.offset)

        val secondChunk = fileData.copyOfRange(100, fileData.size)
        response = mcuManager.fileWrite(
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
