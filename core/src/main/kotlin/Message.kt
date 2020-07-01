package com.juul.mcumgr

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

@JsonIgnoreProperties("group", "command", "operation")
sealed class Request {
    abstract val group: Group
    abstract val command: Command
    abstract val operation: Operation
}

sealed class Response {

    @JsonProperty("rc") val code: Code = Code.Ok

    sealed class Code(@JsonValue val value: Int) {

        object Ok : Code(0)
        object Unknown : Code(1)
        object NoMemory : Code(2)
        object InValue : Code(3)
        object Timeout : Code(4)
        object NoEntry : Code(5)
        object BadState : Code(6)
        object TooLarge : Code(7)
        object NotSupported : Code(8)

        // Primarily used by jackson to decode raw "rc" Int values into a [Code]
        companion object {
            @JvmStatic
            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            fun create(value: Int): Code? = when (value) {
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

    val isSuccess: Boolean = code.isSuccess
    val isError: Boolean = code.isError
}

// // System ////

data class EchoRequest(
    @JsonProperty("d") val echo: String
) : Request() {
    override val operation: Operation = Operation.Write
    override val group: Group = Group.System
    override val command: Command = Command.System.Echo
}

data class EchoResponse(
    @JsonProperty("r") val echo: String
) : Response()

// // Image ////

data class ImageUploadRequest(
    @JsonProperty("data") val data: ByteArray,
    @JsonProperty("off") val offset: Int,
    @JsonProperty("len") val size: Int? = null,
    @JsonProperty("sha") val hash: ByteArray? = null
) : Request() {
    override val operation: Operation = Operation.Write
    override val group: Group = Group.Image
    override val command: Command = Command.Image.Upload
}

data class ImageUploadResponse(
    @JsonProperty("off") val offset: Int
) : Response()

data class CoreDownloadRequest(
    @JsonProperty("off") val offset: Int
) : Request() {
    override val operation: Operation = Operation.Read
    override val group: Group = Group.Image
    override val command: Command = Command.Image.CoreLoad
}

data class CoreDownloadResponse(
    @JsonProperty("data") val data: ByteArray,
    @JsonProperty("off") val offset: Int,
    @JsonProperty("len") val length: Int? = null
) : Response()

// // Files ////

data class FileUploadRequest(
    @JsonProperty("data") val data: ByteArray,
    @JsonProperty("off") val offset: Int,
    @JsonProperty("len") val length: Int? = null
) : Request() {
    override val operation: Operation = Operation.Write
    override val group: Group = Group.Files
    override val command: Command = Command.Files.File
}

data class FileUploadResponse(
    @JsonProperty("off") val offset: Int
) : Response()

data class FileDownloadRequest(
    @JsonProperty("off") val offset: Int
) : Request() {
    override val operation: Operation = Operation.Read
    override val group: Group = Group.Files
    override val command: Command = Command.Files.File
}

data class FileDownloadResponse(
    @JsonProperty("data") val data: ByteArray,
    @JsonProperty("off") val offset: Int,
    @JsonProperty("len") val length: Int? = null
) : Response()

// TODO the rest of the request/responses
