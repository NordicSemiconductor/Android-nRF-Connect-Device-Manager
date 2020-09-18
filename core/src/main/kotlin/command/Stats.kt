package com.juul.mcumgr.command

import com.fasterxml.jackson.annotation.JsonProperty

internal sealed class Stats {

    /*
     * Read
     */

    data class ReadRequest(
        @JsonProperty("name") val name: String
    ) : RequestObject() {

        override val operation: Operation = Operation.Read
        override val group: Group = Group.Stats
        override val command: Command = Command.Stats.Read
    }

    data class ReadResponse(
        @JsonProperty("name") val name: String,
        @JsonProperty("fields") val fields: Map<String, Long>
    ) : ResponseObject() {

        override val group: Group = Group.Stats
        override val command: Command = Command.Stats.Read
    }

    /*
     * List
     */

    object ListRequest : RequestObject() {

        override val operation: Operation = Operation.Read
        override val group: Group = Group.Stats
        override val command: Command = Command.Stats.List
    }

    data class ListResponse(
        @JsonProperty("stat_list") val groups: List<String>
    ) : ResponseObject() {

        override val group: Group = Group.Stats
        override val command: Command = Command.Stats.List
    }
}
