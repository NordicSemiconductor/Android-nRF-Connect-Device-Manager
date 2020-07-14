import com.juul.mcumgr.McuManager
import com.juul.mcumgr.message.Command
import com.juul.mcumgr.message.Format
import com.juul.mcumgr.message.Group
import com.juul.mcumgr.message.Operation
import com.juul.mcumgr.serialization.Message
import mock.MockTransport
import mock.server.Server
import kotlinx.coroutines.runBlocking
import mock.server.FileWriteHandler
import mock.server.Handler
import mock.server.ImageWriteHandler
import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertEquals


class UploaderTest(format: Format) : FormatParameterizedTest(format) {

    private val smallData = Random.Default.nextBytes(1)
    private val defaultData = Random.Default.nextBytes(350_000)
    private val largeData = Random.Default.nextBytes(1_000_000)

    private val defaultCapacity = 5

    private val defaultFileName = "my_file"

    private val mtu = 512
    private val server = Server(mtu, format)
    private val transport = MockTransport(mtu, format, server)
    private val mcuManager = McuManager(transport)

    // Capacity tests

    @Test
    fun `capacity 1 upload success`() = runBlocking {
        mcuManager.uploadImage(defaultData, 1)
        assertByteArrayEquals(defaultData, server.getImageUploadData())
    }

    @Test
    fun `capacity 4 image upload success`() = runBlocking {
        mcuManager.uploadImage(defaultData, 4)
        assertByteArrayEquals(defaultData, server.getImageUploadData())
    }

    @Test
    fun `capacity 16 file upload success`() = runBlocking {
        mcuManager.uploadImage(defaultData, 16)
        assertByteArrayEquals(defaultData, server.getImageUploadData())
    }

    @Test
    fun `capacity 100 file upload success`() = runBlocking {
        mcuManager.uploadImage(defaultData, 100)
        assertByteArrayEquals(defaultData, server.getImageUploadData())
    }

    // Data size tests

    @Test
    fun `empty data size upload success`() = runBlocking {
        // TODO attempt this with a real device and see what happens
    }

    @Test
    fun `small data size upload success`() = runBlocking {
        mcuManager.uploadImage(smallData, defaultCapacity)
        assertByteArrayEquals(smallData, server.getImageUploadData())
    }

    @Test
    fun `large data size upload success`() = runBlocking {
        mcuManager.uploadImage(largeData, defaultCapacity)
        assertByteArrayEquals(largeData, server.getImageUploadData())
    }

    // Implementation tests

    @Test
    fun `image upload success`() = runBlocking {
        mcuManager.uploadImage(defaultData, defaultCapacity)
        assertByteArrayEquals(defaultData, server.getImageUploadData())
    }

    @Test
    fun `file upload success`() = runBlocking {
        mcuManager.uploadFile(defaultData, defaultFileName, defaultCapacity)
        assertByteArrayEquals(defaultData, server.getFileUploadData())
    }
}

fun assertByteArrayEquals(expected: ByteArray, actual: ByteArray) {
    assertEquals(expected.toList(), actual.toList())
}

fun Server.getImageUploadData(): ByteArray {
    val handler =
        checkNotNull(findHandler(Operation.Write, Group.Image, Command.Image.Upload))
    handler as ImageWriteHandler
    return handler.imageData
}

fun Server.getFileUploadData(): ByteArray {
    val handler =
        checkNotNull(findHandler(Operation.Write, Group.Files, Command.Files.File))
    handler as FileWriteHandler
    return handler.fileData
}

