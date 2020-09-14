import com.juul.mcumgr.McuManager
import com.juul.mcumgr.command.Protocol
import com.juul.mcumgr.getOrThrow
import kotlinx.coroutines.runBlocking
import mock.MockTransport
import mock.server.Server
import org.junit.Test
import utils.assertByteArrayEquals
import kotlin.random.Random
import kotlin.test.assertEquals

class FileManagerTest(protocol: Protocol) : ProtocolParameterizedTest(protocol) {

    private val mtu = 512
    private val server = Server(mtu, protocol)
    private val transport = MockTransport(mtu, protocol, server)
    private val mcuManager = McuManager(transport)

    @Test
    fun `file write success`() = runBlocking {
        val fileData = Random.nextBytes(200)

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
