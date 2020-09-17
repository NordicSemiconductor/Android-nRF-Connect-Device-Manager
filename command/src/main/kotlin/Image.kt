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
     * Read/Write Image State
     */

    object ReadStateRequest : Request() {

        override val operation: Operation = Operation.Read
        override val group: Group = Group.Image
        override val command: Command = Command.Image.State
    }

    data class WriteStateRequest(
        @JsonProperty("hash") val hash: ByteArray?,
        @JsonProperty("confirm") val confirm: Boolean
    ) : Request() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.Image
        override val command: Command = Command.Image.State
    }

    data class StateResponse(
        @JsonProperty("images") val images: List<ImageState>
    ) : Response() {

        override val group: Group = Group.Image
        override val command: Command = Command.Image.State

        data class ImageState(
            @JsonProperty("slot") val slot: Int,
            @JsonProperty("version") val version: String,
            @JsonProperty("hash") val hash: ByteArray,
            @JsonProperty("bootable") val bootable: Boolean,
            @JsonProperty("pending") val pending: Boolean,
            @JsonProperty("confirmed") val confirmed: Boolean,
            @JsonProperty("active") val active: Boolean,
            @JsonProperty("permanent") val permanent: Boolean
        )
    }

    /*
     * Image Erase
     */

    object EraseRequest : Request() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.Image
        override val command: Command = Command.Image.Erase
    }

    object EraseResponse : Response() {

        override val group: Group = Group.Image
        override val command: Command = Command.Image.Erase
    }

    /*
     * Image Erase State
     */

    object EraseStateRequest : Request() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.Image
        override val command: Command = Command.Image.EraseState
    }

    object EraseStateResponse : Response() {

        override val group: Group = Group.Image
        override val command: Command = Command.Image.EraseState
    }

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
     * Core List
     */

    object CoreListRequest : Request() {

        override val operation: Operation = Operation.Read
        override val group: Group = Group.Image
        override val command: Command = Command.Image.CoreList
    }

    object CoreListResponse : Response() {

        override val group: Group = Group.Image
        override val command: Command = Command.Image.CoreList
    }

    /*
     * Core Read
     */

    data class CoreReadRequest(
        @JsonProperty("off") val offset: Int
    ) : Request() {

        override val operation: Operation = Operation.Read
        override val group: Group = Group.Image
        override val command: Command = Command.Image.CoreLoad
    }

    data class CoreReadResponse(
        @JsonProperty("data") val data: ByteArray,
        @JsonProperty("off") val offset: Int,
        @JsonProperty("len") val length: Int? = null
    ) : Response() {

        override val group: Group = Group.Image
        override val command: Command = Command.Image.CoreLoad
    }

    /*
     * Core Erase
     */

    object CoreEraseRequest : Request() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.Image
        override val command: Command = Command.Image.CoreLoad
    }

    object CoreEraseResponse : Response() {

        override val group: Group = Group.Image
        override val command: Command = Command.Image.CoreLoad
    }
}
