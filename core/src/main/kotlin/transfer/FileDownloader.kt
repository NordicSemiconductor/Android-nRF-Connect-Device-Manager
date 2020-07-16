package com.juul.mcumgr.transfer

import com.juul.mcumgr.McuManager
import com.juul.mcumgr.McuMgrResult
import com.juul.mcumgr.map

internal class FileDownloader(
    private val fileName: String,
    private val manager: McuManager,
    windowCapacity: Int
) : Downloader(windowCapacity) {

    override suspend fun read(offset: Int): McuMgrResult<Response> =
        manager.fileRead(fileName, offset).map { response ->
            Response(response.data, response.offset, response.length)
        }
}
