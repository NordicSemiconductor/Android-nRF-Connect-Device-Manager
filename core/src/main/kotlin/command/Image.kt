package com.juul.mcumgr.command

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Image group request and response definitions.
 */
internal sealed class Image {

    /*
     * Read/Write Image State
     */

    object ReadStateRequest : RequestObject() {

        override val operation: Operation = Operation.Read
        override val group: Group = Group.Image
        override val command: Command = Command.Image.State
    }

    data class WriteStateRequest(
        @JsonProperty("hash") val hash: ByteArray?,
        @JsonProperty("confirm") val confirm: Boolean
    ) : RequestObject() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.Image
        override val command: Command = Command.Image.State
    }

    data class StateResponse(
        @JsonProperty("images") val images: List<ImageState>
    ) : ResponseObject() {

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

    object EraseRequest : RequestObject() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.Image
        override val command: Command = Command.Image.Erase
    }

    object EraseResponse : ResponseObject() {

        override val group: Group = Group.Image
        override val command: Command = Command.Image.Erase
    }

    /*
     * Image Erase State
     */

    object EraseStateRequest : RequestObject() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.Image
        override val command: Command = Command.Image.EraseState
    }

    object EraseStateResponse : ResponseObject() {

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
    ) : RequestObject() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.Image
        override val command: Command = Command.Image.Upload
    }

    data class ImageWriteResponse(
        @JsonProperty("off") val offset: Int
    ) : ResponseObject() {

        override val group: Group = Group.Image
        override val command: Command = Command.Image.Upload
    }

    /*
     * Core List
     */

    object CoreListRequest : RequestObject() {

        override val operation: Operation = Operation.Read
        override val group: Group = Group.Image
        override val command: Command = Command.Image.CoreList
    }

    object CoreListResponse : ResponseObject() {

        override val group: Group = Group.Image
        override val command: Command = Command.Image.CoreList
    }

    /*
     * Core Read
     */

    data class CoreReadRequest(
        @JsonProperty("off") val offset: Int
    ) : RequestObject() {

        override val operation: Operation = Operation.Read
        override val group: Group = Group.Image
        override val command: Command = Command.Image.CoreLoad
    }

    data class CoreReadResponse(
        @JsonProperty("data") val data: ByteArray,
        @JsonProperty("off") val offset: Int,
        @JsonProperty("len") val length: Int? = null
    ) : ResponseObject() {

        override val group: Group = Group.Image
        override val command: Command = Command.Image.CoreLoad
    }

    /*
     * Core Erase
     */

    object CoreEraseRequest : RequestObject() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.Image
        override val command: Command = Command.Image.CoreLoad
    }

    object CoreEraseResponse : ResponseObject() {

        override val group: Group = Group.Image
        override val command: Command = Command.Image.CoreLoad
    }
}
