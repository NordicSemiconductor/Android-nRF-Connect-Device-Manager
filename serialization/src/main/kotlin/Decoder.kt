package com.juul.mcumgr.serialization

import com.fasterxml.jackson.core.type.TypeReference
import java.io.IOException
import okio.Buffer
import okio.BufferedSource

class DecoderException(
    message: String,
    cause: Throwable?,
    val data: ByteArray,
    val type: String
) : Exception(message, cause)

fun <R> ByteArray.decodeStandard(type: Class<R>): Message<R> = try {
    val buffer = Buffer().write(this)
    val header = buffer.decodeHeader()
    val payload = buffer.decodePayload(type)
    Message(header, payload)
} catch (e: IOException) {
    throw DecoderException(e.message ?: "Failed to decode standard message.", e, this, type.name)
}

fun <R> ByteArray.decodeStandard(reference: TypeReference<R>): Message<R> = try {
    val buffer = Buffer().write(this)
    val header = buffer.decodeHeader()
    val payload = buffer.decodePayload(reference)
    Message(header, payload)
} catch (e: IOException) {
    throw DecoderException(e.message ?: "Failed to decode standard message.", e, this, reference.type.typeName)
}

fun <R> ByteArray.decodeCoap(type: Class<R>): Message<R> = try {
    // Parse the header out of the CBOR map and decode it
    val rawHeader = cbor.readTree(this).get("_h").binaryValue()
    val headerBuffer = Buffer().write(rawHeader)
    val header = headerBuffer.decodeHeader()
    // Decode the whole payload as the message type
    val buffer = Buffer().write(this)
    val payload = buffer.decodePayload(type)
    Message(header, payload)
} catch (e: IOException) {
    throw DecoderException(e.message ?: "Failed to decode coap message.", e, this, type.canonicalName)
}

fun <R> ByteArray.decodeCoap(reference: TypeReference<R>): Message<R> = try {
    // Parse the header out of the CBOR map and decode it
    val rawHeader = cbor.readTree(this).get("_h").binaryValue()
    val headerBuffer = Buffer().write(rawHeader)
    val header = headerBuffer.decodeHeader()
    // Decode the whole payload as the message type
    val buffer = Buffer().write(this)
    val payload = buffer.decodePayload(reference)
    Message(header, payload)
} catch (e: IOException) {
    throw DecoderException(e.message ?: "Failed to decode coap message.", e, this, reference.type.typeName)
}

// Helpers

fun <T> BufferedSource.decodePayload(type: Class<T>) =
    cbor.readValue<T>(inputStream(), type)

fun <T> BufferedSource.decodePayload(reference: TypeReference<T>) =
    cbor.readValue<T>(inputStream(), reference)

fun BufferedSource.decodeHeader(): Header {
    val operation = readByte().toUnsignedInteger()
    val flags = readByte()
    val length = readShort().toUnsignedInteger()
    val group = readShort().toUnsignedInteger()
    val sequenceNumber = readByte().toUnsignedInteger()
    val command = readByte().toUnsignedInteger()
    return Header(operation, group, command, length, sequenceNumber, flags)
}

private fun Byte.toUnsignedInteger(): Int = toInt() and 0xff

private fun Short.toUnsignedInteger(): Int = toInt() and 0xffff
