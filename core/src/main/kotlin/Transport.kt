package com.juul.mcumgr

import com.juul.mcumgr.message.Protocol
import com.juul.mcumgr.message.Request
import com.juul.mcumgr.message.Response

suspend inline fun <reified T : Response> Transport.send(request: Request): McuMgrResult<T> =
    send(request, T::class.java)

interface Transport {

    // TODO both of these are only used in `Uploader`. Maybe remove these to inject elsewhere?
    val mtu: Int
    val protocol: Protocol

    suspend fun <T : Response> send(request: Request, responseType: Class<T>): McuMgrResult<T>
}
