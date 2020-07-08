package com.juul.mcumgr

import com.juul.mcumgr.message.Format
import com.juul.mcumgr.message.Request
import com.juul.mcumgr.message.Response

interface Transport {

    val mtu: Int
    val format: Format

    suspend fun <T : Response> send(request: Request, responseType: Class<T>): McuMgrResult<T>
}
