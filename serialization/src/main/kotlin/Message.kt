package com.juul.mcumgr.serialization



sealed class Message<T> {

    abstract val header: Header
    abstract val payload: T

    data class Standard<T>(
        override val header: Header,
        override val payload: T
    ) : Message<T>()

    data class Coap<T>(
        override val header: Header,
        override val payload: T
    ) : Message<T>()
}

data class Header(
    val operation: Int,
    val group: Int,
    val command: Int,
    val length: Int, // UInt16
    val sequenceNumber: Int, // UInt8
    val flags: Byte
)
