package com.juul.mcumgr.serialization

import com.fasterxml.jackson.databind.node.ObjectNode
import com.juul.mcumgr.Protocol
import com.juul.mcumgr.Request
import okio.Buffer
import okio.BufferedSink

fun Request.encode(protocol: Protocol, sequenceNumber: Int = 0): ByteArray {
    // Convert to the definition to get operation, group, and command for header
    val header = Header(
        operation.value,
        group.value,
        command.value,
        0,
        sequenceNumber,
        0
    )
    val payload: ObjectNode = cbor.valueToTree(this)
    val message = Message(header, payload)
    return message.encode(protocol)
}

fun Message.encode(protocol: Protocol): ByteArray =
    when (protocol) {
        Protocol.SMP -> encodeSmp()
        Protocol.OMP -> encodeOmp()
    }

private fun Message.encodeSmp(): ByteArray =
    Buffer().apply {
        // Set the header's length to the size of the payload.
        val payloadBytes = encodePayload()
        val lengthSetHeader = header.copy(length = payloadBytes.size)
        encodeHeader(lengthSetHeader)
        write(payloadBytes)
    }.readByteArray()

private fun Message.encodeOmp(): ByteArray {
    val payloadBytes = encodePayload()
    val lengthSetHeader = header.copy(length = payloadBytes.size)
    val headerBytes = Buffer().apply {
        // Set the header's length to the size of the payload.
        encodeHeader(lengthSetHeader)
    }
    payload.put("_h", headerBytes.readByteArray())
    return encodePayload()
}

// Helpers

private fun Message.encodePayload(): ByteArray =
    cbor.writeValueAsBytes(payload)

private fun BufferedSink.encodeHeader(header: Header) {
    writeByte(header.operation)
    writeByte(header.flags.toInt())
    writeShort(header.length)
    writeShort(header.group)
    writeByte(header.sequenceNumber)
    writeByte(header.command)
}
