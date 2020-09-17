package com.juul.mcumgr.command

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * File group request and response definitions.
 */
internal sealed class File {

    /*
     * File Write
     */

    data class WriteRequest(
        @JsonProperty("name") val fileName: String,
        @JsonProperty("data") val data: ByteArray,
        @JsonProperty("off") val offset: Int,
        @JsonProperty("len") val length: Int? = null
    ) : RequestObject() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.File
        override val command: Command = Command.File.File
    }

    data class WriteResponse(
        @JsonProperty("off") val offset: Int
    ) : ResponseObject() {

        override val group: Group = Group.File
        override val command: Command = Command.File.File
    }

    /*
     * File Read
     */

    data class ReadRequest(
        @JsonProperty("name") val fileName: String,
        @JsonProperty("off") val offset: Int
    ) : RequestObject() {

        override val operation: Operation = Operation.Read
        override val group: Group = Group.File
        override val command: Command = Command.File.File
    }

    data class ReadResponse(
        @JsonProperty("data") val data: ByteArray,
        @JsonProperty("off") val offset: Int,
        @JsonProperty("len") val length: Int? = null
    ) : ResponseObject() {

        override val group: Group = Group.File
        override val command: Command = Command.File.File
    }
}
