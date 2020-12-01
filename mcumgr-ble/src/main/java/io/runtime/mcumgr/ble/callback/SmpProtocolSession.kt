package io.runtime.mcumgr.ble.callback

import android.os.Handler
import io.runtime.mcumgr.McuMgrHeader
import io.runtime.mcumgr.ble.util.RotatingCounter
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.EmptyCoroutineContext

private const val SMP_SEQ_NUM_MAX = 255

internal class SmpProtocolSession(
    private val handler: Handler? = null
) {

    private data class Outgoing(val data: ByteArray, val transaction: SmpTransaction)

    private val scope = CoroutineScope(EmptyCoroutineContext)
    private val txChannel: Channel<Outgoing> = Channel(SMP_SEQ_NUM_MAX + 1)
    private val rxChannel: Channel<ByteArray> = Channel(SMP_SEQ_NUM_MAX + 1)
    private val sequenceCounter = RotatingCounter(SMP_SEQ_NUM_MAX)
    private val transactions: Array<SmpTransaction?> = arrayOfNulls(SMP_SEQ_NUM_MAX + 1)
    private val transactionsMutex = Mutex()

    /**
     * Launches the main coroutine and channel consumers.
     */
    init {
        scope.launch(
            // When the session is closed, fail all remaining transactions.
            // Exception is propagated from close through the channels.
            CoroutineExceptionHandler { _, throwable ->
                for (i in transactions.indices) {
                    fail(i, throwable)
                }
            }
        ) {
            // Launch the reader and writer
            launch { reader() }
            launch { writer() }
        }
    }

    fun send(data: ByteArray, transaction: SmpTransaction) {
        check(txChannel.offer(Outgoing(data, transaction))) {
            "Cannot send request, transmit channel buffer is full."
        }
    }

    fun receive(data: ByteArray) {
        check(rxChannel.offer(data)) {
            "Cannot receive response, receive channel buffer is full."
        }
    }

    /**
     * Consumes messages off the tx channel until the channel is closed.
     */
    private suspend fun writer() {
        txChannel.consumeEach { outgoing ->
            // Set sequence number in outgoing data
            val sequenceNumber = sequenceCounter.getAndRotate()
            outgoing.data.setSequenceNumber(sequenceNumber)

            // Add transaction to store. Fail an existing transaction on overwrite
            transactionsMutex.withLock {
                fail(sequenceNumber, TransactionOverwriteException(sequenceNumber))
                transactions[sequenceNumber] = outgoing.transaction
            }

            // Send the transaction and launch timeout coroutine
            outgoing.transaction.send(handler, outgoing.data)
            scope.launch {
                delay(10000)
                fail(sequenceNumber, TransactionTimeoutException(sequenceNumber))
            }
        }
    }

    /**
     * Consumes messages of the rx channel until the channel is closed.
     */
    private suspend fun reader() {
        rxChannel.consumeEach { data ->
            // Parse header to get sequence number
            val header = McuMgrHeader.fromBytes(data)
            val sequenceNumber = header.sequenceNum

            // Get the transaction from the store and call the callback
            val transaction = transactionsMutex.withLock {
                transactions[sequenceNumber]
            }
            transaction?.onResponse(handler, data)
        }
    }

    fun close(e: Exception) {
        txChannel.close(e)
        rxChannel.close(e)
    }

    private fun fail(id: Int, e: Throwable) {
        transactions[id]?.let { transaction ->
            transactions[id] = null
            transaction.onFailure(handler, e)
        }
    }

    private fun ByteArray.setSequenceNumber(value: Int) {
        this[6] = (value and 0xff).toByte()
    }
}

private fun SmpTransaction.send(handler: Handler?, data: ByteArray) {
    when (handler) {
        null -> send(data)
        else -> handler.post { send(data) }
    }
}

private fun SmpTransaction.onResponse(handler: Handler?, data: ByteArray) {
    when (handler) {
        null -> onResponse(data)
        else -> handler.post { onResponse(data) }
    }
}

private fun SmpTransaction.onFailure(handler: Handler?, e: Throwable) {
    when (handler) {
        null -> onFailure(e)
        else -> handler.post { onFailure(e) }
    }
}
