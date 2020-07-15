package com.juul.mcumgr.serialization

import com.fasterxml.jackson.databind.node.ObjectNode
import com.juul.mcumgr.McuMgrResult
import com.juul.mcumgr.message.Response
import java.io.IOException

data class Message(
    val header: Header,
    val payload: ObjectNode
)

fun <T> Message.toResult(type: Class<T>): McuMgrResult<T> {
    val rawCode = payload["rc"].asInt(-1)
    val code = Response.Code.valueOf(rawCode) ?: Response.Code.Ok
    return try {
        val response = cbor.treeToValue(payload, type)
        McuMgrResult.Success(response, code)
    } catch (e: IOException) {
        // Failed to parse full response. Try to get code.
        when (code) {
            Response.Code.Ok -> McuMgrResult.Failure(e)
            else -> McuMgrResult.Error(code)
        }
    }
}

data class Header(
    val operation: Int,
    val group: Int,
    val command: Int,
    val length: Int, // UInt16
    val sequenceNumber: Int, // UInt8
    val flags: Byte
)
