package com.juul.mcumgr.transfer

import com.juul.mcumgr.command.CommandResult
import com.juul.mcumgr.ImageManager
import com.juul.mcumgr.Transport
import com.juul.mcumgr.command.mapResponse
import okio.ByteString.Companion.toByteString

class ImageUploader(
    data: ByteArray,
    transport: Transport,
    windowCapacity: Int = 1
) : Uploader(
    data,
    windowCapacity,
    transport.mtu,
    transport.protocol,
    0
) {

    private val imageManager = ImageManager(transport)

    private val truncatedHash =
        data.toByteString().sha256().toByteArray().copyOfRange(0, TRUNCATED_HASH_LEN)

    override suspend fun write(
        data: ByteArray,
        offset: Int,
        length: Int?
    ): CommandResult<Response> {
        // Send the truncated hash
        val hash = if (offset == 0) {
            truncatedHash
        } else {
            null
        }
        return imageManager.imageWrite(data, offset, length, hash).mapResponse { response ->
            Response(response.offset)
        }
    }
}
