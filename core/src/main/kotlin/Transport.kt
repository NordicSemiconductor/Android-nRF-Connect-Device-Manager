package com.juul.mcumgr

interface Transport {

    enum class Scheme {
        STANDARD,
        COAP;
    }

    val mtu: Int
    val scheme: Scheme

    @Throws
    suspend fun <T : Response> send(request: Request, responseType: Class<T>): T
}
