package io.runtime.mcumgr.ble.callback

import java.util.concurrent.TimeoutException

class TransactionTimeoutException internal constructor(
    val id: Int
) : TimeoutException("Transaction $id timed out without receiving a response") {

    override fun toString(): String {
        return message!!
    }
}

class TransactionOverwriteException internal constructor(
    val id: Int
) : Exception("Transaction $id has been overwritten") {

    override fun toString(): String {
        return message!!
    }
}

internal interface SmpTransaction {
    fun send(data: ByteArray)
    fun onResponse(data: ByteArray)
    fun onFailure(e: Throwable)
}
