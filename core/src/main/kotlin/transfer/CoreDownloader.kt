package com.juul.mcumgr.transfer

import com.juul.mcumgr.ImageManager
import com.juul.mcumgr.Transport
import com.juul.mcumgr.command.CommandResult
import com.juul.mcumgr.command.mapResponse

class CoreDownloader(
    transport: Transport,
    windowCapacity: Int = 1
) : Downloader(windowCapacity) {

    private val imageManager = ImageManager(transport)

    override suspend fun read(offset: Int): CommandResult<Response> =
        imageManager.coreRead(offset).mapResponse { response ->
            Response(response.data, response.offset, response.length)
        }
}
