package com.juul.mcumgr.transfer

import com.juul.mcumgr.McuManager
import com.juul.mcumgr.McuMgrResult
import com.juul.mcumgr.map

class FileUploader(
    data: ByteArray,
    private val fileName: String,
    private val manager: McuManager,
    windowCapacity: Int = 1
) : Uploader(
    data,
    windowCapacity,
    manager.transport.mtu,
    manager.transport.format,
    cborStringLength("name") + cborStringLength(fileName)
) {

    override suspend fun write(
        data: ByteArray,
        offset: Int,
        length: Int?
    ): McuMgrResult<Response> =
        manager.fileWrite(fileName, data, offset, length).map { response ->
            Response(response.offset)
        }
}
