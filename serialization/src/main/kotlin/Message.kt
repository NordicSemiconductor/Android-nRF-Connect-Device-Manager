package com.juul.mcumgr.serialization

import com.fasterxml.jackson.databind.node.ObjectNode
import com.juul.mcumgr.SendResult
import com.juul.mcumgr.command.ResponseCode
import java.io.IOException

data class Message(
    val header: Header,
    val payload: ObjectNode
)

data class Header(
    val operation: Int,
    val group: Int,
    val command: Int,
    val length: Int, // UInt16
    val sequenceNumber: Int, // UInt8
    val flags: Byte
)

fun <T> Message.toResult(type: Class<T>): SendResult<T> {
    check(isResponse) { "cannot transform request to result" }
    val rawCode = payload["rc"].asInt(-1)
    val code = ResponseCode.valueOf(rawCode) ?: ResponseCode.Ok
    return try {
        val response = cbor.treeToValue(payload, type)
        SendResult.Response(response, code)
    } catch (e: IOException) {
        // Failed to parse full response. If rc is not present or indicates success, then we know
        // something went wrong (i.e. this is not a normal error response).
        when (rawCode) {
            -1, 0 -> SendResult.Failure(e)
            else -> SendResult.Response(null, code)
        }
    }
}

private val Message.isResponse: Boolean
    get() = header.operation == Operation.ReadResponse.value ||
        header.operation == Operation.WriteResponse.value
