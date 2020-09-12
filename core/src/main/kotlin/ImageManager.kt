package com.juul.mcumgr

import com.juul.mcumgr.message.CoreReadRequest
import com.juul.mcumgr.message.CoreReadResponse
import com.juul.mcumgr.message.ImageWriteRequest
import com.juul.mcumgr.message.ImageWriteResponse
import com.juul.mcumgr.transfer.CoreDownloader
import com.juul.mcumgr.transfer.ImageUploader

class ImageManager(val transport: Transport) {

    suspend fun imageWrite(
        data: ByteArray,
        offset: Int,
        length: Int? = null,
        hash: ByteArray? = null
    ): SendResult<ImageWriteResponse> =
        transport.send(ImageWriteRequest(data, offset, length, hash))

    suspend fun coreRead(offset: Int): SendResult<CoreReadResponse> =
        transport.send(CoreReadRequest(offset))

    fun imageUploader(
        data: ByteArray,
        windowCapacity: Int = 1
    ) = ImageUploader(data, transport, windowCapacity)

    fun coreDownloader(
        windowCapacity: Int = 1
    ) = CoreDownloader(transport, windowCapacity)
}
