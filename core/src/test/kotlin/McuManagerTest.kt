package com.juul.mcumgr

import com.juul.mcumgr.McuMgrResult.Error
import com.juul.mcumgr.McuMgrResult.Failure
import com.juul.mcumgr.McuMgrResult.Success
import com.juul.mcumgr.message.Format
import com.juul.mcumgr.message.Response
import com.juul.mcumgr.mock.MockTransport
import com.juul.mcumgr.mock.server.EchoHandler
import com.juul.mcumgr.mock.server.Server
import com.juul.mcumgr.mock.server.toErrorResponseHandler
import com.juul.mcumgr.mock.server.toThrowHandler
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(value = Parameterized::class)
class McuManagerTest(
    private val format: Format
) {

    private val mtu = 512
    private val server = Server(mtu, format)
    private val transport = MockTransport(mtu, format, server)
    private val mcuManager = McuManager(transport)

    @Test
    fun `error response result expected`() = runBlocking {
        server.overrides.add(EchoHandler().toErrorResponseHandler(Response.Code.BadState))
        when (val result = mcuManager.echo("test")) {
            is Error -> assertEquals(Response.Code.BadState, result.code)
            is Success, is Failure -> error("expected McuMgrResult.Error, got $result")
        }
    }

    @Test
    fun `failure result expected`() = runBlocking {
        val expectedException = RuntimeException("This should come back in failure result")
        server.overrides.add(EchoHandler().toThrowHandler(expectedException))
        when (val result = mcuManager.echo("test")) {
            is Failure -> assertEquals(expectedException, result.throwable)
            is Success, is Error -> error("expected failure exception, got $result")
        }
    }

    @Test
    fun `echo success`() = runBlocking {
        val echo = "Hello McuManager!"
        val response = mcuManager.echo(echo).getOrThrow()
        assertEquals(echo, response.echo)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: format={0}")
        fun data(): Iterable<Format> {
            return listOf(
                Format.SMP,
                Format.OMP
            )
        }
    }
}
