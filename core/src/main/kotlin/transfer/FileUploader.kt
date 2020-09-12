package com.juul.mcumgr.transfer

import com.juul.mcumgr.FileManager
import com.juul.mcumgr.SendResult
import com.juul.mcumgr.Transport
import com.juul.mcumgr.mapResponse

class FileUploader(
    private val fileName: String,
    data: ByteArray,
    transport: Transport,
    windowCapacity: Int = 1
) : Uploader(
    data,
    windowCapacity,
    transport.mtu,
    transport.protocol,
    // File upload contains an extra field, name, which will mess up chunk size computations unless
    // we add the value to the packet overhead.
    cborStringLength("name") + cborStringLength(fileName)
) {

    private val fileManager = FileManager(transport)

    override suspend fun write(
        data: ByteArray,
        offset: Int,
        length: Int?
    ): SendResult<Response> =
        fileManager.fileWrite(fileName, data, offset, length).mapResponse { response ->
            Response(response.offset)
        }
}
