package no.nordicsemi.android.mcumgr.ble

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nordicsemi.android.ble.exception.DeviceDisconnectedException
import no.nordicsemi.android.mcumgr.McuManager
import no.nordicsemi.android.mcumgr.McuMgrHeader
import no.nordicsemi.android.mcumgr.McuMgrScheme
import no.nordicsemi.android.mcumgr.ble.callback.SmpProtocolSession
import no.nordicsemi.android.mcumgr.ble.callback.SmpTransaction
import no.nordicsemi.android.mcumgr.ble.callback.TransactionTimeoutException
import no.nordicsemi.android.mcumgr.response.McuMgrResponse
import no.nordicsemi.android.mcumgr.response.dflt.McuMgrEchoResponse
import no.nordicsemi.android.mcumgr.util.CBOR
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SmpProtocolSessionTest {

    private val session = SmpProtocolSession()

    private abstract class TestTransaction : SmpTransaction {

        val result = Channel<ByteArray>(Channel.RENDEZVOUS)

        override fun onResponse(data: ByteArray) {
            result.trySend(data)
        }

        override fun onFailure(e: Throwable) {
            result.close(e)
        }
    }

    private val echoTransaction = object : TestTransaction() {

        override fun send(data: ByteArray) {
            val header = McuMgrHeader.fromBytes(data)
            val payload = data.copyOfRange(8, data.size)
            val echo = CBOR.getString(payload, "d")
            val response = McuManager.buildPacket(
                McuMgrScheme.BLE,
                3, header.flags, header.groupId, header.sequenceNum, header.commandId,
                mapOf("r" to echo)
            )
            session.receive(response)
        }
    }

    @Test
    fun `send and receive, success`() = runBlocking {
        val echo = "Hello!"
        val request = McuManager.buildPacket(
            McuMgrScheme.BLE,
            0, 0, 0, 0, 0,
            mapOf("d" to echo)
        )
        session.send(request, 40_000, echoTransaction)
        val responseData = echoTransaction.result.receive()
        val response = McuMgrResponse.buildResponse(
            McuMgrScheme.BLE,
            responseData,
            McuMgrEchoResponse::class.java
        )
        assertEquals(echo, response.r)
    }

    @Test
    fun `send and receive, success, no timeout`() = runBlocking {
        val echo = "Hello!"
        val request = McuManager.buildPacket(
            McuMgrScheme.BLE,
            0, 0, 0, 0, 0,
            mapOf("d" to echo)
        )
        session.send(request, 40_000, echoTransaction)
        val responseData = echoTransaction.result.receive()
        val response = McuMgrResponse.buildResponse(
            McuMgrScheme.BLE,
            responseData,
            McuMgrEchoResponse::class.java
        )
        assertEquals(echo, response.r)
        delay(SmpProtocolSession.TIMEOUT + 1_000) // Wait longer than the timeout
    }

    @Test
    fun `send, counter increase and rollover`() = runBlocking {
        val echo = "Hello!"
        val request = newEchoRequest(echo)
        repeat(256) { i ->
            session.send(request, 40_000, echoTransaction)
            val responseData = echoTransaction.result.receive()
            val response = McuMgrResponse.buildResponse(
                McuMgrScheme.BLE,
                responseData,
                McuMgrEchoResponse::class.java
            )
            assertEquals(echo, response.r)
            val expected = if (i < 256) {
                i
            } else {
                0
            }
            assertEquals(expected, response.header?.sequenceNum)
        }
    }

    @Test
    fun `send, response timeout`() = runBlocking {
        val echo = "Hello!"
        val request = newEchoRequest(echo)
        val transaction = object : TestTransaction() {
            override fun send(data: ByteArray) {}
        }
        session.send(request, 40_000, transaction)
        assertFailsWith(TransactionTimeoutException::class) {
            transaction.result.receive()
        }
        Unit
    }

    @Test
    fun `close fails active transactions`() = runBlocking {
        val echo = "Hello!"
        val request = newEchoRequest(echo)
        val transaction = object : TestTransaction() {
            override fun send(data: ByteArray) {}
        }
        session.send(request, 40_000, transaction)
        launch {
            delay(1000)
            session.close(DeviceDisconnectedException())
        }
        assertFailsWith(DeviceDisconnectedException::class) {
            transaction.result.receive()
        }
        Unit
    }

    @Test
    fun `send overflows transmit buffer`() = runBlocking {
        val echo = "Hello!"
        val request = newEchoRequest(echo)
        val transaction = object : TestTransaction() {
            override fun send(data: ByteArray) {
                runBlocking { delay (10000) }
            }
        }
        // First request is consumed which blocks the channel, remaining 256
        // fills the channel buffer to capacity
        repeat(257) {
            launch {
                session.send(request, 40_000, transaction)
            }
        }
        launch {
            assertFailsWith(IllegalStateException::class) {
                session.send(request, 40_000, transaction)
            }
        }
        Unit
    }
}

private fun newEchoRequest(echo: String): ByteArray {
    return McuManager.buildPacket(
        McuMgrScheme.BLE,
        0, 0, 0, 0, 0,
        mapOf("d" to echo)
    )
}
