import com.juul.mcumgr.ImageManager
import com.juul.mcumgr.Protocol
import com.juul.mcumgr.getOrThrow
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import mock.MockTransport
import mock.server.Server
import org.junit.Test
import utils.assertByteArrayEquals

class ImageManagerTest(protocol: Protocol) : ProtocolParameterizedTest(protocol) {

    private val mtu = 512
    private val server = Server(mtu, protocol)
    private val transport = MockTransport(mtu, protocol, server)
    private val manager = ImageManager(transport)

    @Test
    fun `image write success`() = runBlocking {
        val imageData = Random.nextBytes(200)

        val firstChunk = imageData.copyOfRange(0, 100)
        var response = manager.imageWrite(
            firstChunk,
            0,
            200
        ).getOrThrow()
        assertEquals(100, response.offset)

        val secondChunk = imageData.copyOfRange(100, imageData.size)
        response = manager.imageWrite(
            secondChunk,
            100
        ).getOrThrow()
        assertEquals(200, response.offset)
        assertByteArrayEquals(imageData, server.getImageUploadData())
    }
}
