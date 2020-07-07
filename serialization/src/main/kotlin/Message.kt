package com.juul.mcumgr.serialization

data class Message<T>(
    val header: Header,
    val payload: T
)

data class Header(
    val operation: Int,
    val group: Int,
    val command: Int,
    val length: Int, // UInt16
    val sequenceNumber: Int, // UInt8
    val flags: Byte
)
