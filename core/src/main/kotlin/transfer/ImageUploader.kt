package com.juul.mcumgr.transfer

import com.juul.mcumgr.McuManager
import com.juul.mcumgr.McuMgrResult
import com.juul.mcumgr.map
import com.juul.mcumgr.message.Format

class ImageUploader(val manager: McuManager) : Uploader {

    override val mtu: Int = manager.transport.mtu
    override val format: Format = manager.transport.format

    override suspend fun write(
        data: ByteArray,
        offset: Int,
        length: Int
    ): McuMgrResult<Uploader.Response> =
        manager.imageUpload(data, offset, length).map { response ->
            Uploader.Response(response.offset)
        }
}
