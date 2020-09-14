package com.juul.mcumgr

import com.juul.mcumgr.command.Protocol
import com.juul.mcumgr.command.Request
import com.juul.mcumgr.command.Response

suspend inline fun <reified T : Response> Transport.send(request: Request): SendResult<T> =
    send(request, T::class.java)

interface Transport {

    // TODO both of these are only used in `Uploader`. Maybe remove these to inject elsewhere?
    val mtu: Int
    val protocol: Protocol

    suspend fun <T : Response> Transport.send(request: Request, responseType: Class<T>): SendResult<T>
}
