import com.juul.mcumgr.CommandResult
import com.juul.mcumgr.McuManager
import com.juul.mcumgr.Protocol
import com.juul.mcumgr.getOrThrow
import com.juul.mcumgr.transfer.FileUploader
import com.juul.mcumgr.transfer.ImageUploader
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import mock.MockTransport
import mock.server.FileWriteHandler
import mock.server.ImageWriteHandler
import mock.server.Server
import mock.server.toThrowHandler
import org.junit.Test
import utils.ExpectedException
import utils.assertByteArrayEquals

class UploaderTest(protocol: Protocol) : ProtocolParameterizedTest(protocol) {

    private val smallData = Random.Default.nextBytes(1)
    private val defaultData = Random.Default.nextBytes(350_000)
    private val largeData = Random.Default.nextBytes(1_000_000)

    private val defaultCapacity = 5
    private val defaultFileName = "my_file"

    private val mtu = 512
    private val server = Server(mtu, protocol)
    private val transport = MockTransport(mtu, protocol, server)
    private val mcuManager = McuManager(transport)

    // Capacity tests

    @Test
    fun `capacity 1 upload success`() = runBlocking {
        mcuManager.uploadImage(defaultData, 1).getOrThrow()
        assertByteArrayEquals(defaultData, server.getImageUploadData())
    }

    @Test
    fun `capacity 4 upload success`() = runBlocking {
        mcuManager.uploadImage(defaultData, 4).getOrThrow()
        assertByteArrayEquals(defaultData, server.getImageUploadData())
    }

    @Test
    fun `capacity 16 upload success`() = runBlocking {
        mcuManager.uploadImage(defaultData, 16).getOrThrow()
        assertByteArrayEquals(defaultData, server.getImageUploadData())
    }

    @Test
    fun `capacity 100 upload success`() = runBlocking {
        mcuManager.uploadImage(defaultData, 100).getOrThrow()
        assertByteArrayEquals(defaultData, server.getImageUploadData())
    }

    // Data size tests

    @Test
    fun `empty data size upload success`() = runBlocking {
        // TODO attempt this with a real device and see what happens
    }

    @Test
    fun `small data size upload success`() = runBlocking {
        mcuManager.uploadImage(smallData, defaultCapacity).getOrThrow()
        assertByteArrayEquals(smallData, server.getImageUploadData())
    }

    @Test
    fun `large data size upload success`() = runBlocking {
        mcuManager.uploadImage(largeData, defaultCapacity).getOrThrow()
        assertByteArrayEquals(largeData, server.getImageUploadData())
    }

    // Implementation tests

    @Test
    fun `image upload success`() = runBlocking {
        mcuManager.uploadImage(defaultData, defaultCapacity).getOrThrow()
        assertByteArrayEquals(defaultData, server.getImageUploadData())
    }

    @Test
    fun `image upload failure`() = runBlocking {
        server.overrides.add(ImageWriteHandler().toThrowHandler(ExpectedException))
        val result = mcuManager.uploadImage(defaultData, defaultCapacity)
        result as CommandResult.Failure
        assertEquals(ExpectedException, result.throwable)
    }

    @Test
    fun `file upload success`() = runBlocking {
        mcuManager.uploadFile(defaultFileName, defaultData, defaultCapacity).getOrThrow()
        assertByteArrayEquals(
            defaultData,
            server.getFileUploadData(defaultFileName)
        )
    }

    @Test
    fun `file upload failure`() = runBlocking {
        server.overrides.add(FileWriteHandler().toThrowHandler(ExpectedException))
        val result = mcuManager.uploadFile(defaultFileName, defaultData, defaultCapacity)
        result as CommandResult.Failure
        assertEquals(ExpectedException, result.throwable)
    }
}

suspend fun McuManager.uploadImage(
    data: ByteArray,
    windowCapacity: Int = 1
): CommandResult<Unit> {
    val uploader = ImageUploader(data, transport, windowCapacity)
    return catchResult {
        uploader.upload()
    }
}

suspend fun McuManager.uploadFile(
    fileName: String,
    data: ByteArray,
    windowCapacity: Int = 1
): CommandResult<Unit> {
    val uploader = FileUploader(fileName, data, transport, windowCapacity)
    return catchResult {
        uploader.upload()
    }
}

fun Server.getImageUploadData(): ByteArray {
    val handler: ImageWriteHandler = findHandler()
    return handler.imageData
}

fun Server.getFileUploadData(fileName: String): ByteArray {
    val handler: FileWriteHandler = findHandler()
    return checkNotNull(handler.files[fileName]) { "file $fileName does not exist" }
}
