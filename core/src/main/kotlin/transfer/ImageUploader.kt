package com.juul.mcumgr.transfer

import com.juul.mcumgr.McuManager
import com.juul.mcumgr.McuMgrResult
import com.juul.mcumgr.map
import okio.ByteString.Companion.toByteString


class ImageUploader(
    data: ByteArray,
    val manager: McuManager,
    windowCapacity: Int = 1
) : Uploader(
    data,
    windowCapacity,
    manager.transport.mtu,
    manager.transport.format,
    0
) {

    private val truncatedHash =
        data.toByteString().sha256().toByteArray().copyOfRange(0, TRUNCATED_HASH_LEN)

    override suspend fun write(
        data: ByteArray,
        offset: Int,
        length: Int?
    ): McuMgrResult<Response> {
        // Send the truncated hash
        val hash = if (offset == 0) {
            truncatedHash
        } else {
            null
        }
        return manager.imageWrite(data, offset, length, hash).map { response ->
            Response(response.offset)
        }
    }
}
