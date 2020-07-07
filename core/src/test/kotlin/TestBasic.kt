package com.juul.mcumgr

import com.juul.mcumgr.mock.MockTransport
import com.juul.mcumgr.mock.server.Server
import kotlinx.coroutines.runBlocking
import org.junit.Test

class TestBasic {

    private val scheme = Transport.Scheme.STANDARD
    private val mtu = 512
    private val server = Server(mtu, scheme)
    private val transport = MockTransport(mtu, scheme, server)
    private val mcuManager = McuManager(transport)

    @Test
    fun test() = runBlocking {
        val response = mcuManager.echo("Hello McuManager!").getOrThrow()
        println(response)
    }
}
