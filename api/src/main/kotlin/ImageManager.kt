package com.juul.mcumgr

import com.juul.mcumgr.command.Image
import com.juul.mcumgr.transfer.CoreDownloader
import com.juul.mcumgr.transfer.ImageUploader
import com.juul.mcumgr.util.mapResponse

data class ImageWriteResponse(
    val offset: Int
)

data class CoreReadResponse(
    val data: ByteArray,
    val offset: Int,
    val length: Int? = null
)

class ImageManager(val transport: Transport) {

    suspend fun imageWrite(
        data: ByteArray,
        offset: Int,
        length: Int? = null,
        hash: ByteArray? = null
    ): CommandResult<ImageWriteResponse> {
        val request = Image.ImageWriteRequest(data, offset, length, hash)
        return transport.send(
            request,
            Image.ImageWriteResponse::class.java
        ).mapResponse { response ->
            ImageWriteResponse(response.offset)
        }
    }

    suspend fun coreRead(offset: Int): CommandResult<CoreReadResponse> {
        val request = Image.CoreReadRequest(offset)
        return transport.send(
            request,
            Image.CoreReadResponse::class.java
        ).mapResponse { response ->
            CoreReadResponse(response.data, response.offset, response.length)
        }
    }

    fun imageUploader(
        data: ByteArray,
        windowCapacity: Int = 1
    ) = ImageUploader(data, transport, windowCapacity)

    fun coreDownloader(
        windowCapacity: Int = 1
    ) = CoreDownloader(transport, windowCapacity)
}
