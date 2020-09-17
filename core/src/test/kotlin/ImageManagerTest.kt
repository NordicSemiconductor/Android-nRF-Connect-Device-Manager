import com.juul.mcumgr.ImageManager
import com.juul.mcumgr.Protocol
import com.juul.mcumgr.command.ResponseCode
import com.juul.mcumgr.command.Image
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import mock.MockTransport
import mock.server.Server
import mock.server.handler.CoreEraseHandler
import mock.server.handler.CoreListHandler
import mock.server.handler.CoreReadHandler
import mock.server.handler.ImageEraseHandler
import mock.server.handler.ImageEraseStateHandler
import mock.server.handler.ImageStateHandler
import org.junit.After
import org.junit.Test
import util.assertByteArrayEquals
import util.assertResponseCode
import util.getOrAssert
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImageManagerTest(protocol: Protocol) : ProtocolParameterizedTest(protocol) {

    private val mtu = 512
    private val server = Server(mtu, protocol)
    private val transport = MockTransport(mtu, protocol, server)
    private val manager = ImageManager(transport)

    private val images: MutableList<Image.StateResponse.ImageState> = mutableListOf(
        Image.StateResponse.ImageState(
            slot = 0,
            version = "1.2.3",
            hash = Random.nextBytes(32),
            bootable = true,
            pending = false,
            confirmed = true,
            active = true,
            permanent = false
        ),
        Image.StateResponse.ImageState(
            slot = 1,
            version = "1.2.4",
            hash = Random.nextBytes(32),
            bootable = true,
            pending = false,
            confirmed = false,
            active = false,
            permanent = false
        )
    )

    @After
    fun `reset server`() {
        server.reset()
    }

    @Test
    fun `image list, success`() = runBlocking {
        val handler: ImageStateHandler = server.findHandler()
        handler.images = images
        val response = manager.list().getOrAssert()
        response.images.forEachIndexed { i, responseState ->
            val expectedState = images[i]
            assertEquals(expectedState.slot, responseState.slot)
            assertEquals(expectedState.version, responseState.version)
            assertByteArrayEquals(expectedState.hash, responseState.hash)
            assertEquals(expectedState.bootable, responseState.bootable)
            assertEquals(expectedState.pending, responseState.pending)
            assertEquals(expectedState.confirmed, responseState.confirmed)
            assertEquals(expectedState.active, responseState.active)
            assertEquals(expectedState.permanent, responseState.permanent)
        }
    }

    @Test
    fun `image test, success`() = runBlocking {
        val handler: ImageStateHandler = server.findHandler()
        handler.images = images
        val response = manager.test(images[1].hash).getOrAssert()
        response.images.forEachIndexed { i, responseState ->
            val expectedState = images[i]
            assertEquals(expectedState.slot, responseState.slot)
            assertEquals(expectedState.version, responseState.version)
            assertByteArrayEquals(expectedState.hash, responseState.hash)
            assertEquals(expectedState.bootable, responseState.bootable)
            if (i == 1) {
                assertEquals(true, responseState.pending)
            } else {
                assertEquals(expectedState.pending, responseState.pending)

            }
            assertEquals(expectedState.confirmed, responseState.confirmed)
            assertEquals(expectedState.active, responseState.active)
            assertEquals(expectedState.permanent, responseState.permanent)
        }
    }

    @Test
    fun `image confirm, success`() = runBlocking {
        val handler: ImageStateHandler = server.findHandler()
        handler.images = images
        val response = manager.confirm().getOrAssert()
        response.images.forEachIndexed { i, responseState ->
            val expectedState = images[i]
            assertEquals(expectedState.slot, responseState.slot)
            assertEquals(expectedState.version, responseState.version)
            assertByteArrayEquals(expectedState.hash, responseState.hash)
            assertEquals(expectedState.bootable, responseState.bootable)
            assertEquals(expectedState.pending, responseState.pending)
            if (i == 0) {
                assertEquals(true, responseState.confirmed)
            } else {
                assertEquals(expectedState.confirmed, responseState.confirmed)
            }
            assertEquals(expectedState.active, responseState.active)
            assertEquals(expectedState.permanent, responseState.permanent)
        }
    }

    @Test
    fun `image erase, success`() = runBlocking {
        val handler: ImageEraseHandler = server.findHandler()
        handler.imageData = Random.nextBytes(100)
        manager.erase().getOrAssert()
        assertNull(handler.imageData)
    }

    @Test
    fun `image state erase, success`() = runBlocking {
        val handler: ImageEraseStateHandler = server.findHandler()
        manager.eraseState().getOrAssert()
        assertTrue(handler.erased)
    }

    @Test
    fun `image write, success`() = runBlocking {
        val imageData = Random.nextBytes(200)

        val firstChunk = imageData.copyOfRange(0, 100)
        var response = manager.imageWrite(
            firstChunk,
            0,
            200
        ).getOrAssert()
        assertEquals(100, response.offset)

        val secondChunk = imageData.copyOfRange(100, imageData.size)
        response = manager.imageWrite(
            secondChunk,
            100
        ).getOrAssert()
        assertEquals(200, response.offset)
        assertByteArrayEquals(imageData, server.getImageUploadData())
    }

    @Test
    fun `core list, success`() = runBlocking {
        val handler: CoreListHandler = server.findHandler()
        manager.coreList().assertResponseCode(ResponseCode.NoEntry)
        handler.coreData = Random.nextBytes(100)
        manager.coreList().getOrAssert()
    }

    @Test
    fun `core read, success`() = runBlocking {
        val coreData = Random.nextBytes(200)
        val handler: CoreReadHandler = server.findHandler()
        manager.coreRead(0).assertResponseCode(ResponseCode.NoEntry)

        var offset = 0
        val chunkSize = 100

        handler.coreData = coreData
        handler.chunkSize = chunkSize

        while (offset < coreData.size) {
            val response = manager.coreRead(offset).getOrAssert()
            assertByteArrayEquals(
                coreData.copyOfRange(offset, offset+chunkSize),
                response.data
            )
            offset += response.data.size
        }
    }

    @Test
    fun `core erase, success`() = runBlocking {
        val handler: CoreEraseHandler = server.findHandler()
        handler.coreData = Random.nextBytes(100)
        manager.coreErase().getOrAssert()
        assertNull(handler.coreData)
    }
}
