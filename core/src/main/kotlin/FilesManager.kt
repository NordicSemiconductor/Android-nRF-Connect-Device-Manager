package com.juul.mcumgr

import com.juul.mcumgr.message.Request
import com.juul.mcumgr.message.Response
import com.juul.mcumgr.transfer.FileDownloader
import com.juul.mcumgr.transfer.FileUploader


data class FileWriteRequest(
    val fileName: String,
    val data: ByteArray,
    val offset: Int,
    val length: Int? = null
) : Request()

data class FileWriteResponse(
    val offset: Int
) : Response()

data class FileReadRequest(
    val fileName: String,
    val offset: Int
) : Request()

data class FileReadResponse(
    val data: ByteArray,
    val offset: Int,
    val length: Int? = null
) : Response()

class FilesManager(val transport: Transport) {

    suspend fun fileWrite(
        fileName: String,
        data: ByteArray,
        offset: Int,
        length: Int? = null
    ): McuMgrResult<FileWriteResponse> {
        return transport.send(FileWriteRequest(fileName, data, offset, length))
    }

    suspend fun fileRead(fileName: String, offset: Int): McuMgrResult<FileReadResponse> =
        transport.send(FileReadRequest(fileName, offset))

    fun fileUploader(
        data: ByteArray,
        fileName: String,
        windowCapacity: Int = 1
    ) = FileUploader(fileName, data, transport, windowCapacity)

    fun fileDownloader(
        fileName: String,
        windowCapacity: Int = 1
    ) = FileDownloader(fileName, transport, windowCapacity)
}
