import com.juul.mcumgr.CommandResult
import com.juul.mcumgr.ErrorResponseException
import com.juul.mcumgr.McuManager
import com.juul.mcumgr.Protocol
import com.juul.mcumgr.ResponseCode
import com.juul.mcumgr.transfer.CoreDownloader
import com.juul.mcumgr.transfer.FileDownloader
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import mock.MockTransport
import mock.server.handler.CoreReadHandler
import mock.server.handler.FileReadHandler
import mock.server.Server
import org.junit.Test
import util.assertByteArrayEquals
import util.getOrAssert

class DownloaderTest(protocol: Protocol) : ProtocolParameterizedTest(protocol) {

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
    fun `capacity 1 download success`() = runBlocking {
        server.setCoreData(defaultData)
        val data = mcuManager.downloadCore(1).getOrAssert()
        assertByteArrayEquals(defaultData, data)
    }

    @Test
    fun `capacity 4 download success`() = runBlocking {
        server.setCoreData(defaultData)
        val data = mcuManager.downloadCore(4).getOrAssert()
        assertByteArrayEquals(defaultData, data)
    }

    @Test
    fun `capacity 16 download success`() = runBlocking {
        server.setCoreData(defaultData)
        val data = mcuManager.downloadCore(16).getOrAssert()
        assertByteArrayEquals(defaultData, data)
    }

    @Test
    fun `capacity 100 download success`() = runBlocking {
        server.setCoreData(defaultData)
        val data = mcuManager.downloadCore(100).getOrAssert()
        assertByteArrayEquals(defaultData, data)
    }

    // Data size tests

    @Test
    fun `no data, core download error response`() = runBlocking {
        server.setCoreData(null)
        val result = mcuManager.downloadCore(defaultCapacity)
        result as CommandResult.Response
        assertEquals(ResponseCode.NoEntry, result.code)
    }

    @Test
    fun `no data, file download error response`() = runBlocking {
        server.setFileData(defaultFileName, null)
        val result = mcuManager.downloadFile(defaultFileName, defaultCapacity)
        result as CommandResult.Response
        assertEquals(ResponseCode.NoEntry, result.code)
    }

    @Test
    fun `small data size download success`() = runBlocking {
        server.setCoreData(smallData)
        val data = mcuManager.downloadCore(defaultCapacity).getOrAssert()
        assertByteArrayEquals(smallData, data)
    }

    @Test
    fun `large data size download success`() = runBlocking {
        server.setCoreData(largeData)
        val data = mcuManager.downloadCore(defaultCapacity).getOrAssert()
        assertByteArrayEquals(largeData, data)
    }

    // Implementation tests

    @Test
    fun `core download success`() = runBlocking {
        server.setCoreData(defaultData)
        val data = mcuManager.downloadCore(defaultCapacity).getOrAssert()
        assertByteArrayEquals(defaultData, data)
    }

    @Test
    fun `file download success`() = runBlocking {
        server.setFileData(defaultFileName, defaultData)
        val data = mcuManager.downloadFile(defaultFileName, defaultCapacity).getOrAssert()
        assertByteArrayEquals(defaultData, data)
    }
}

suspend fun McuManager.downloadCore(
    windowCapacity: Int = 1
): CommandResult<ByteArray> {
    val downloader = CoreDownloader(transport, windowCapacity)
    return catchResult {
        downloader.download()
    }
}

suspend fun McuManager.downloadFile(
    fileName: String,
    windowCapacity: Int = 1
): CommandResult<ByteArray> {
    val downloader = FileDownloader(fileName, transport, windowCapacity)
    return catchResult {
        downloader.download()
    }
}

fun Server.setCoreData(data: ByteArray?) {
    val handler: CoreReadHandler = findHandler()
    handler.coreData = data
}

fun Server.setFileData(fileName: String, data: ByteArray?) {
    val handler: FileReadHandler = findHandler()
    if (data == null) {
        handler.files.remove(fileName)
    } else {
        handler.files[fileName] = data
    }
}

internal inline fun <T, R> T.catchResult(block: T.() -> R): CommandResult<R> {
    return try {
        CommandResult.Response(block(), ResponseCode.Ok)
    } catch (e: ErrorResponseException) {
        CommandResult.Response(null, e.code)
    } catch (e: Throwable) {
        CommandResult.Failure(e)
    }
}
