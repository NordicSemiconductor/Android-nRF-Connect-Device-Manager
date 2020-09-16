package com.juul.mcumgr

interface Transport {

    // TODO both of these are only used in `Uploader`. Maybe remove these to inject elsewhere?
    val mtu: Int
    val protocol: Protocol

    suspend fun <T : Response> send(request: Request, responseType: Class<T>): CommandResult<T>
}
