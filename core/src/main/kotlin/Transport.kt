package com.juul.mcumgr

import com.juul.mcumgr.message.Format
import com.juul.mcumgr.message.Request
import com.juul.mcumgr.message.Response

suspend inline fun <reified T : Response> Transport.send(request: Request): McuMgrResult<T> =
    send(request, T::class.java)

interface Transport {

    val mtu: Int
    val format: Format

    suspend fun <T : Response> send(request: Request, responseType: Class<T>): McuMgrResult<T>
}
