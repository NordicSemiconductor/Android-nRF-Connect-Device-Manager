package com.juul.mcumgr.command

import com.fasterxml.jackson.annotation.JsonProperty
import com.juul.mcumgr.Command
import com.juul.mcumgr.Group
import com.juul.mcumgr.Operation

/**
 * Image group request and response definitions.
 */
sealed class Image {

    /*
     * Image Write
     */

    data class ImageWriteRequest(
        @JsonProperty("data") val data: ByteArray,
        @JsonProperty("off") val offset: Int,
        @JsonProperty("len") val size: Int? = null,
        @JsonProperty("sha") val hash: ByteArray? = null
    ) : Request() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.Image
        override val command: Command = Command.Image.Upload
    }

    data class ImageWriteResponse(
        @JsonProperty("off") val offset: Int
    ) : Response() {

        override val group: Group = Group.Image
        override val command: Command = Command.Image.Upload
    }

    /*
     * Core Read
     */

    data class CoreReadRequest(
        @JsonProperty("off") val offset: Int
    ) : Request() {

        override val operation: Operation = Operation.Read
        override val group: Group = Group.Image
        override val command: Command = Command.Image.CoreDownload
    }

    data class CoreReadResponse(
        @JsonProperty("data") val data: ByteArray,
        @JsonProperty("off") val offset: Int,
        @JsonProperty("len") val length: Int? = null
    ) : Response() {

        override val group: Group = Group.Image
        override val command: Command = Command.Image.CoreDownload
    }
}
