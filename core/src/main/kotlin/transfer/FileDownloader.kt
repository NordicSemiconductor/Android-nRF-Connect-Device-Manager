package com.juul.mcumgr.transfer

import com.juul.mcumgr.McuManager
import com.juul.mcumgr.McuMgrResult
import com.juul.mcumgr.map

class FileDownloader(val manager: McuManager) : Downloader {

    override suspend fun read(offset: Int): McuMgrResult<Downloader.Response> =
        manager.fileDownload(offset).map { response ->
            Downloader.Response(response.data, response.offset, response.length)
        }
}
