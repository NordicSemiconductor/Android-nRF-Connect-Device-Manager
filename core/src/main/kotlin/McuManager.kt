package com.juul.mcumgr

import com.juul.mcumgr.message.CoreReadRequest
import com.juul.mcumgr.message.CoreReadResponse
import com.juul.mcumgr.message.EchoRequest
import com.juul.mcumgr.message.EchoResponse
import com.juul.mcumgr.message.FileReadRequest
import com.juul.mcumgr.message.FileReadResponse
import com.juul.mcumgr.message.FileWriteRequest
import com.juul.mcumgr.message.FileWriteResponse
import com.juul.mcumgr.message.ImageWriteRequest
import com.juul.mcumgr.message.ImageWriteResponse
import com.juul.mcumgr.message.Request
import com.juul.mcumgr.message.Response
import com.juul.mcumgr.transfer.CoreDownloader
import com.juul.mcumgr.transfer.Downloader
import com.juul.mcumgr.transfer.FileDownloader
import com.juul.mcumgr.transfer.FileUploader
import com.juul.mcumgr.transfer.ImageUploader
import com.juul.mcumgr.transfer.Uploader
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class McuManager(val transport: Transport) {

    // System

    suspend fun echo(echo: String): McuMgrResult<EchoResponse> =
        send(EchoRequest(echo))

    // Image

    suspend fun uploadImage(
        data: ByteArray,
        windowCapacity: Int = 1,
        progressHandler: ((Uploader.Progress) -> Unit)? = null
    ) {
        val uploader = ImageUploader(data, this, windowCapacity)
        upload(uploader, progressHandler)
    }

    suspend fun downloadCore(
        windowCapacity: Int = 1,
        progressHandler: ((Downloader.Progress) -> Unit)? = null
    ): ByteArray {
        val downloader = CoreDownloader(this, windowCapacity)
        return download(downloader, progressHandler)
    }

    suspend fun imageWrite(
        data: ByteArray,
        offset: Int,
        length: Int?,
        hash: ByteArray?
    ): McuMgrResult<ImageWriteResponse> =
        send(ImageWriteRequest(data, offset, length, hash))

    suspend fun coreRead(offset: Int): McuMgrResult<CoreReadResponse> =
        send(CoreReadRequest(offset))

    // Files

    suspend fun uploadFile(
        data: ByteArray,
        fileName: String,
        windowCapacity: Int = 1,
        progressHandler: ((Uploader.Progress) -> Unit)? = null
    ) {
        val uploader = FileUploader(data, fileName, this, windowCapacity)
        upload(uploader, progressHandler)
    }

    suspend fun downloadFile(
        fileName: String,
        windowCapacity: Int = 1,
        progressHandler: ((Downloader.Progress) -> Unit)? = null
    ): ByteArray {
        val downloader = FileDownloader(fileName, this, windowCapacity)
        return download(downloader, progressHandler)
    }

    suspend fun fileWrite(
        fileName: String,
        data: ByteArray,
        offset: Int,
        length: Int?
    ): McuMgrResult<FileWriteResponse> {
        return send(FileWriteRequest(fileName, data, offset, length))
    }

    suspend fun fileRead(fileName: String, offset: Int): McuMgrResult<FileReadResponse> =
        send(FileReadRequest(fileName, offset))

    // Send

    suspend inline fun <reified T : Response> send(request: Request): McuMgrResult<T> = try {
        transport.send(request, T::class.java)
    } catch (t: Throwable) {
        McuMgrResult.Failure(t)
    }

    // Transfer

    private suspend fun upload(
        uploader: Uploader,
        progressHandler: ((Uploader.Progress) -> Unit)? = null
    ) {
        coroutineScope {
            val job = uploader.progress.onEach { progress ->
                progressHandler?.invoke(progress)
            }.launchIn(this)
            uploader.upload()
            job.cancel()
        }
    }

    private suspend fun download(
        downloader: Downloader,
        progressHandler: ((Downloader.Progress) -> Unit)? = null
    ): ByteArray {
        return coroutineScope {
            val job = downloader.progress.onEach { progress ->
                progressHandler?.invoke(progress)
            }.launchIn(this)
            val data = downloader.download()
            job.cancel()
            data
        }
    }
}


