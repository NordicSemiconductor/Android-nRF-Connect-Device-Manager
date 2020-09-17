import com.juul.mcumgr.McuManager
import com.juul.mcumgr.Protocol
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import mock.MockTransport
import mock.server.Server
import org.junit.Test
import util.assertByteArrayEquals
import util.getOrAssert

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
        ).getOrAssert()
        assertEquals(100, response.offset)

        val secondChunk = fileData.copyOfRange(100, fileData.size)
        response = mcuManager.files.fileWrite(
            "my_file",
            secondChunk,
            100
        ).getOrAssert()
        assertEquals(200, response.offset)
        assertByteArrayEquals(
            fileData,
            server.getFileUploadData("my_file")
        )
    }
}
