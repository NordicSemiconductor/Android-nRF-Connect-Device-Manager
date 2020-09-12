package com.juul.mcumgr.transfer

import com.juul.mcumgr.FileManager
import com.juul.mcumgr.SendResult
import com.juul.mcumgr.Transport
import com.juul.mcumgr.mapResponse

class FileDownloader(
    private val fileName: String,
    transport: Transport,
    windowCapacity: Int
) : Downloader(windowCapacity) {

    private val fileManager = FileManager(transport)

    override suspend fun read(offset: Int): SendResult<Response> =
        fileManager.fileRead(fileName, offset).mapResponse { response ->
            Response(response.data, response.offset, response.length)
        }
}
