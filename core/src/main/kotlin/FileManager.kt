package com.juul.mcumgr

import com.juul.mcumgr.message.FileReadRequest
import com.juul.mcumgr.message.FileReadResponse
import com.juul.mcumgr.message.FileWriteRequest
import com.juul.mcumgr.message.FileWriteResponse
import com.juul.mcumgr.transfer.FileDownloader
import com.juul.mcumgr.transfer.FileUploader

class FileManager(val transport: Transport) {

    suspend fun fileWrite(
        fileName: String,
        data: ByteArray,
        offset: Int,
        length: Int? = null
    ): SendResult<FileWriteResponse> {
        return transport.send(FileWriteRequest(fileName, data, offset, length))
    }

    suspend fun fileRead(fileName: String, offset: Int): SendResult<FileReadResponse> =
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
