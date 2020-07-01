package com.juul.mcumgr.serialization

import java.io.IOException
import okio.Buffer
import okio.BufferedSource

class DecoderException(
    message: String,
    cause: Throwable,
    val data: ByteArray,
    val type: String
) : Exception(message, cause)

interface Decoder {
    @Throws(DecoderException::class)
    fun <T> decode(data: ByteArray, type: Class<T>): Message<T>
}

class StandardSchemeDecoder : Decoder {

    @Throws(DecoderException::class)
    override fun <T> decode(data: ByteArray, type: Class<T>): Message<T> = try {
        val buffer = Buffer().write(data)
        val header = buffer.decodeHeader()
        val payload = buffer.decodePayload(type)
        Message(header, payload)
    } catch (e: IOException) {
        throw DecoderException(e.message ?: "Failed to decode message.", e, data, type.name)
    }
}

class CoapSchemeDecoder : Decoder {

    @Throws(DecoderException::class)
    override fun <T> decode(data: ByteArray, type: Class<T>): Message<T> = try {
        // Parse the header out of the CBOR map and decode it
        val rawHeader = cbor.readTree(data).get("_h").binaryValue()
        val headerBuffer = Buffer().write(rawHeader)
        val header = headerBuffer.decodeHeader()
        // Decode the whole payload as the message type
        val buffer = Buffer().write(data)
        val payload = buffer.decodePayload(type)
        Message(header, payload)
    } catch (e: IOException) {
        throw DecoderException(e.message ?: "Failed to decode message.", e, data, type.name)
    }
}

// Helpers

private fun <T> BufferedSource.decodePayload(type: Class<T>) =
    cbor.readValue<T>(inputStream(), type)

private fun BufferedSource.decodeHeader(): Header {
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
