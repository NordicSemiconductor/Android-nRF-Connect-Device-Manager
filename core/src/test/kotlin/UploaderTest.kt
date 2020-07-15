import com.juul.mcumgr.McuManager
import com.juul.mcumgr.McuMgrResult
import com.juul.mcumgr.getOrThrow
import com.juul.mcumgr.message.Command
import com.juul.mcumgr.message.Format
import com.juul.mcumgr.message.Group
import com.juul.mcumgr.message.Operation
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
        result as McuMgrResult.Failure
        assertEquals(ExpectedException, result.throwable)
    }

    @Test
    fun `file upload success`() = runBlocking {
        mcuManager.uploadFile(defaultData, defaultFileName, defaultCapacity).getOrThrow()
        assertByteArrayEquals(
            defaultData,
            server.getFileUploadData(defaultFileName)
        )
    }

    @Test
    fun `file upload failure`() = runBlocking {
        server.overrides.add(FileWriteHandler().toThrowHandler(ExpectedException))
        val result = mcuManager.uploadFile(defaultData, defaultFileName, defaultCapacity)
        result as McuMgrResult.Failure
        assertEquals(ExpectedException, result.throwable)
    }
}

fun Server.getImageUploadData(): ByteArray {
    val handler =
        checkNotNull(findHandler(Operation.Write, Group.Image, Command.Image.Upload))
    handler as ImageWriteHandler
    return handler.imageData
}

fun Server.getFileUploadData(fileName: String): ByteArray {
    val handler =
        checkNotNull(findHandler(Operation.Write, Group.Files, Command.Files.File))
    handler as FileWriteHandler
    return checkNotNull(handler.files[fileName]) { "file $fileName does not exist" }
}
