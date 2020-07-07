package com.juul.mcumgr.serialization

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.node.ObjectNode
import okio.Buffer
import okio.BufferedSink

class EncoderException(
    message: String,
    cause: Throwable,
    val obj: Message<*>
) : Exception(message, cause)

fun Message<*>.encodeStandard(): ByteArray = try {
    val payload = Buffer().apply {
        encodePayload(payload ?: Any())
    }
    val header = Buffer().apply {
        // Set the header's length to the size of the payload.
        val lengthSetHeader = header.copy(length = payload.size.toInt())
        encodeHeader(lengthSetHeader)
    }
    Buffer().apply {
        writeAll(header)
        writeAll(payload)
    }.readByteArray()
} catch (e: JsonProcessingException) {
    throw EncoderException("Failed to encode message.", e, this)
}

fun Message<*>.encodeCoap(): ByteArray = try {
    val payloadBuf = Buffer().apply {
        encodePayload(payload ?: Any())
    }
    val headerBuf = Buffer().apply {
        // Set the header's length to the size of the payload.
        val lengthSetHeader = header.copy(length = payloadBuf.size.toInt())
        encodeHeader(lengthSetHeader)
    }

    // Parse the object as a object tree and insert the header as a field.
    val objectMapper = cbor
    val objectNode = objectMapper.valueToTree<ObjectNode>(payload).apply {
        put("_h", headerBuf.readByteArray())
    }
    objectMapper.writeValueAsBytes(objectNode)
} catch (e: JsonProcessingException) {
    throw EncoderException("Failed to encode message.", e, this)
}

// Helpers

fun BufferedSink.encodePayload(payload: Any) {
    write(cbor.writeValueAsBytes(payload))
}

fun BufferedSink.encodeHeader(header: Header) {
    writeByte(header.operation)
    writeByte(header.flags.toInt())
    writeShort(header.length)
    writeShort(header.group)
    writeByte(header.sequenceNumber)
    writeByte(header.command)
}
