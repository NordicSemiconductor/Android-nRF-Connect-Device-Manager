package com.juul.mcumgr.message

sealed class Request {
    abstract val group: Group
    abstract val command: Command
    abstract val operation: Operation
}

sealed class Response {

    sealed class Code(val value: Int) {

        object Ok : Code(0)
        object Unknown : Code(1)
        object NoMemory : Code(2)
        object InValue : Code(3)
        object Timeout : Code(4)
        object NoEntry : Code(5)
        object BadState : Code(6)
        object TooLarge : Code(7)
        object NotSupported : Code(8)

        companion object {
            @JvmStatic
            fun valueOf(value: Int): Code? = when (value) {
                0 -> Ok
                1 -> Unknown
                2 -> NoMemory
                3 -> InValue
                4 -> Timeout
                5 -> NoEntry
                6 -> BadState
                7 -> TooLarge
                8 -> NotSupported
                else -> null
            }
        }

        val isSuccess: Boolean get() = this is Ok
        val isError: Boolean get() = !isSuccess
    }
}

// System

data class EchoRequest(
    val echo: String
) : Request() {
    override val operation: Operation = Operation.Write
    override val group: Group = Group.System
    override val command: Command = Command.System.Echo
}

data class EchoResponse(
    val echo: String
) : Response()

// Image

data class ImageWriteRequest(
    val data: ByteArray,
    val offset: Int,
    val size: Int? = null,
    val hash: ByteArray? = null
) : Request() {
    override val operation: Operation = Operation.Write
    override val group: Group = Group.Image
    override val command: Command = Command.Image.Upload
}

data class ImageWriteResponse(
    val offset: Int
) : Response()

data class CoreReadRequest(
    val offset: Int
) : Request() {
    override val operation: Operation = Operation.Read
    override val group: Group = Group.Image
    override val command: Command = Command.Image.CoreLoad
}

data class CoreReadResponse(
    val data: ByteArray,
    val offset: Int,
    val length: Int? = null
) : Response()

// Files

data class FileWriteRequest(
    val fileName: String,
    val data: ByteArray,
    val offset: Int,
    val length: Int? = null
) : Request() {
    override val operation: Operation = Operation.Write
    override val group: Group = Group.Files
    override val command: Command = Command.Files.File
}

data class FileWriteResponse(
    val offset: Int
) : Response()

data class FileReadRequest(
    val fileName: String,
    val offset: Int
) : Request() {
    override val operation: Operation = Operation.Read
    override val group: Group = Group.Files
    override val command: Command = Command.Files.File
}

data class FileReadResponse(
    val data: ByteArray,
    val offset: Int,
    val length: Int? = null
) : Response()

// TODO the rest of the request/responses
