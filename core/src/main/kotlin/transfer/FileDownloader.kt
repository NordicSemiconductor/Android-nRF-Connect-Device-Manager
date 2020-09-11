package com.juul.mcumgr.transfer

import com.juul.mcumgr.FilesManager
import com.juul.mcumgr.McuManager
import com.juul.mcumgr.McuMgrResult
import com.juul.mcumgr.Transport
import com.juul.mcumgr.map

class FileDownloader(
    private val fileName: String,
    transport: Transport,
    windowCapacity: Int
) : Downloader(windowCapacity) {

    private val fileManager = FilesManager(transport)

    override suspend fun read(offset: Int): McuMgrResult<Response> =
        fileManager.fileRead(fileName, offset).map { response ->
            Response(response.data, response.offset, response.length)
        }
}
