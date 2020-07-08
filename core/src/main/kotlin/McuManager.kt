package com.juul.mcumgr

import com.juul.mcumgr.message.CoreDownloadRequest
import com.juul.mcumgr.message.CoreDownloadResponse
import com.juul.mcumgr.message.EchoRequest
import com.juul.mcumgr.message.EchoResponse
import com.juul.mcumgr.message.FileDownloadRequest
import com.juul.mcumgr.message.FileDownloadResponse
import com.juul.mcumgr.message.FileUploadResponse
import com.juul.mcumgr.message.ImageUploadRequest
import com.juul.mcumgr.message.ImageUploadResponse
import com.juul.mcumgr.message.Request
import com.juul.mcumgr.message.Response

class McuManager(val transport: Transport) {

    // System

    suspend fun echo(echo: String): McuMgrResult<EchoResponse> =
        send(EchoRequest(echo))

    // Image

    suspend fun imageUpload(data: ByteArray, offset: Int, length: Int): McuMgrResult<ImageUploadResponse> =
        send(ImageUploadRequest(data, offset, length))

    suspend fun coreDownload(offset: Int): McuMgrResult<CoreDownloadResponse> =
        send(CoreDownloadRequest(offset))

    // Files

    suspend fun fileUpload(data: ByteArray, offset: Int, length: Int): McuMgrResult<FileUploadResponse> =
        send(ImageUploadRequest(data, offset, length))

    suspend fun fileDownload(offset: Int): McuMgrResult<FileDownloadResponse> =
        send(FileDownloadRequest(offset))

    suspend inline fun <reified T : Response> send(request: Request): McuMgrResult<T> = try {
        transport.send(request, T::class.java)
    } catch (t: Throwable) {
        McuMgrResult.Failure(t)
    }
}
