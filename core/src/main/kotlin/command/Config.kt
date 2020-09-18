package com.juul.mcumgr.command

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Config group request and response definitions.
 */
internal sealed class Config {

    /*
     * Config Read
     */

    data class ReadRequest(
        @JsonProperty("name") val name: String
    ) : RequestObject() {

        override val operation: Operation = Operation.Read
        override val group: Group = Group.Config
        override val command: Command = Command.Config.Config

    }

    data class ReadResponse(
        @JsonProperty("val") val value: String
    ) : ResponseObject() {

        override val group: Group = Group.Config
        override val command: Command = Command.Config.Config
    }

    /*
     * Config Write
     */

    data class WriteRequest(
        @JsonProperty("name") val name: String,
        @JsonProperty("val") val value: String,
        @JsonProperty("save") val save: Boolean
    ) : RequestObject() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.Config
        override val command: Command = Command.Config.Config
    }

    object WriteResponse : ResponseObject() {

        override val group: Group = Group.Config
        override val command: Command = Command.Config.Config
    }
}
