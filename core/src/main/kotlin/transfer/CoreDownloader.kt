package com.juul.mcumgr.transfer

import com.juul.mcumgr.ImageManager
import com.juul.mcumgr.McuMgrResult
import com.juul.mcumgr.Transport
import com.juul.mcumgr.map

class CoreDownloader(
    transport: Transport,
    windowCapacity: Int = 1
) : Downloader(windowCapacity) {

    private val imageManager = ImageManager(transport)

    override suspend fun read(offset: Int): McuMgrResult<Response> =
        imageManager.coreRead(offset).map { response ->
            Response(response.data, response.offset, response.length)
        }
}
