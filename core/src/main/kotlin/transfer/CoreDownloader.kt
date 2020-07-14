package com.juul.mcumgr.transfer

import com.juul.mcumgr.McuManager
import com.juul.mcumgr.McuMgrResult
import com.juul.mcumgr.map

class CoreDownloader(
    private val manager: McuManager,
    windowCapacity: Int = 1
) : Downloader(windowCapacity) {

    override suspend fun read(offset: Int): McuMgrResult<Response> =
        manager.coreRead(offset).map { response ->
            Response(response.data, response.offset, response.length)
        }
}
