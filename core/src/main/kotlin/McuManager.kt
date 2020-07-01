package com.juul.mcumgr

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
        Success(transport.send(request, T::class.java))
    } catch (t: Throwable) {
        Failure(t)
    }
}
