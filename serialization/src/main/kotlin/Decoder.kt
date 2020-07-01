package com.juul.mcumgr.serialization

import com.fasterxml.jackson.core.type.TypeReference
import java.io.IOException
import okio.Buffer
import okio.BufferedSource

class DecoderException(
    message: String,
    cause: Throwable?,
    val data: ByteArray,
    val type: String?
) : Exception(message, cause)

inline fun <reified T : Message<R>, reified R> ByteArray.decode(): Message<R> {
    return when (T::class) {
        Message.Standard::class -> decodeStandard()
        Message.Coap::class -> decodeCoap()
        else -> {
            throw DecoderException("Unknown message class ${T::class.java}", null, this, R::class.qualifiedName)
        }
    }
}
inline fun <reified R> ByteArray.decodeStandard(): Message.Standard<R> = try {
    val buffer = Buffer().write(this)
    val header = buffer.decodeHeader()
    val payload = buffer.decodePayload(object : TypeReference<R>() {})
    Message.Standard(header, payload)
} catch (e: IOException) {
    throw DecoderException(e.message ?: "Failed to decode standard message.", e, this, R::class.qualifiedName)
}

inline fun <reified R> ByteArray.decodeCoap(): Message.Coap<R> = try {
    // Parse the header out of the CBOR map and decode it
    val rawHeader = cbor.readTree(this).get("_h").binaryValue()
    val headerBuffer = Buffer().write(rawHeader)
    val header = headerBuffer.decodeHeader()
    // Decode the whole payload as the message type
    val buffer = Buffer().write(this)
    val payload = buffer.decodePayload(object : TypeReference<R>() {})
    Message.Coap(header, payload)
} catch (e: IOException) {
    throw DecoderException(e.message ?: "Failed to decode coap message.", e, this, R::class.qualifiedName)
}

// Helpers

fun <T> BufferedSource.decodePayload(type: TypeReference<T>) =
    cbor.readValue<T>(inputStream(), type)

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
