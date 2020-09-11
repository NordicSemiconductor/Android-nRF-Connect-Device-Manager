package com.juul.mcumgr.serialization

import com.fasterxml.jackson.annotation.JsonProperty
import com.juul.mcumgr.message.Command
import com.juul.mcumgr.message.Group
import com.juul.mcumgr.message.Operation

/**
 * System group request and response definitions.
 */
sealed class System {

    internal data class EchoRequest(
        @JsonProperty("d") val echo: String
    ) : RequestDefinition() {
        override val operation: Operation = Operation.Write
        override val group: Group = Group.System
        override val command: Command = Command.System.Echo
    }

    internal data class EchoResponse(
        @JsonProperty("r") val echo: String
    ) : ResponseDefinition()
}

/**
 * Image group request and response definitions.
 */
sealed class Image {

    internal data class ImageWriteRequest(
        @JsonProperty("data") val data: ByteArray,
        @JsonProperty("off") val offset: Int,
        @JsonProperty("len") val size: Int? = null,
        @JsonProperty("sha") val hash: ByteArray? = null
    ) : RequestDefinition() {
        override val operation: Operation = Operation.Write
        override val group: Group = Group.Image
        override val command: Command = Command.Image.Upload
    }

    internal data class ImageWriteResponse(
        @JsonProperty("off") val offset: Int
    ) : ResponseDefinition()

    internal data class CoreReadRequest(
        @JsonProperty("off") val offset: Int
    ) : RequestDefinition() {
        override val operation: Operation = Operation.Read
        override val group: Group = Group.Image
        override val command: Command = Command.Image.CoreLoad
    }

    internal data class CoreReadResponse(
        @JsonProperty("data") val data: ByteArray,
        @JsonProperty("off") val offset: Int,
        @JsonProperty("len") val length: Int? = null
    ) : ResponseDefinition()
}

/**
 * File group request and response definitions.
 */
internal sealed class File {

    internal data class WriteRequest(
        @JsonProperty("name") val fileName: String,
        @JsonProperty("data") val data: ByteArray,
        @JsonProperty("off") val offset: Int,
        @JsonProperty("len") val length: Int? = null
    ) : RequestDefinition() {
        override val operation: Operation = Operation.Write
        override val group: Group = Group.Files
        override val command: Command = Command.Files.File
    }

    internal data class WriteResponse(
        @JsonProperty("off") val offset: Int
    ) : ResponseDefinition()

    internal data class ReadRequest(
        @JsonProperty("name") val fileName: String,
        @JsonProperty("off") val offset: Int
    ) : RequestDefinition() {
        override val operation: Operation = Operation.Read
        override val group: Group = Group.Files
        override val command: Command = Command.Files.File
    }

    internal data class ReadResponse(
        @JsonProperty("data") val data: ByteArray,
        @JsonProperty("off") val offset: Int,
        @JsonProperty("len") val length: Int? = null
    ) : ResponseDefinition()
}



