package com.juul.mcumgr

import com.juul.mcumgr.message.Request
import com.juul.mcumgr.message.Response
import com.juul.mcumgr.transfer.CoreDownloader
import com.juul.mcumgr.transfer.ImageUploader

data class ImageWriteRequest(
    val data: ByteArray,
    val offset: Int,
    val size: Int? = null,
    val hash: ByteArray? = null
) : Request()

data class ImageWriteResponse(
    val offset: Int
) : Response()

data class CoreReadRequest(
    val offset: Int
) : Request()

data class CoreReadResponse(
    val data: ByteArray,
    val offset: Int,
    val length: Int? = null
) : Response()

/**
 * Manager
 */
class ImageManager(val transport: Transport) {

    suspend fun imageWrite(
        data: ByteArray,
        offset: Int,
        length: Int? = null,
        hash: ByteArray? = null
    ): McuMgrResult<ImageWriteResponse> =
        transport.send(ImageWriteRequest(data, offset, length, hash))

    suspend fun coreRead(offset: Int): McuMgrResult<CoreReadResponse> =
        transport.send(CoreReadRequest(offset))

    fun imageUploader(
        data: ByteArray,
        windowCapacity: Int = 1
    ) = ImageUploader(data, transport, windowCapacity)

    fun coreDownloader(
        windowCapacity: Int = 1
    ) = CoreDownloader(transport, windowCapacity)
}
