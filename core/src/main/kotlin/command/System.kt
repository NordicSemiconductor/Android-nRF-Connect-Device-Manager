package com.juul.mcumgr.command

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * System group request and response definitions.
 */
internal sealed class System {

    /*
     * Echo
     */

    data class EchoRequest(
        @JsonProperty("d") val echo: String
    ) : RequestObject() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.System
        override val command: Command = Command.System.Echo
    }

    data class EchoResponse(
        @JsonProperty("r") val echo: String
    ) : ResponseObject() {

        override val group: Group = Group.System
        override val command: Command = Command.System.Echo
    }

    /*
     * Console Echo Control
     */

    data class ConsoleEchoControlRequest(
        @JsonProperty("echo") val enabled: Boolean
    ) : RequestObject() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.System
        override val command: Command = Command.System.ConsoleEchoControl
    }

    object ConsoleEchoControlResponse : ResponseObject() {

        override val group: Group = Group.System
        override val command: Command = Command.System.ConsoleEchoControl
    }

    /*
     * Task Stats
     */

    object TaskStatsRequest : RequestObject() {

        override val operation: Operation = Operation.Read
        override val group: Group = Group.System
        override val command: Command = Command.System.TaskStats
    }

    data class TaskStatsResponse(
        @JsonProperty("tasks") val tasks: Map<String, Task>
    ) : ResponseObject() {

        override val group: Group = Group.System
        override val command: Command = Command.System.TaskStats

        data class Task(
            @JsonProperty("prio") val priority: Int,
            @JsonProperty("tid") val taskId: Int,
            @JsonProperty("state") val state: Int,
            @JsonProperty("stkuse") val stackUse: Int,
            @JsonProperty("stksiz") val stackSize: Int,
            @JsonProperty("cswcnt") val contextSwitchCount: Int,
            @JsonProperty("runtime") val runtime: Int,
            @JsonProperty("last_checkin") val lastCheckIn: Int,
            @JsonProperty("next_checkin") val nextCheckIn: Int
        )
    }

    /*
     * Memory Pool Stats
     */

    object MemoryPoolStatsRequest : RequestObject() {

        override val operation: Operation = Operation.Read
        override val group: Group = Group.System
        override val command: Command = Command.System.MemoryPoolStats
    }

    data class MemoryPoolStatsResponse(
        @JsonProperty("mpools") val memoryPools: Map<String, MemoryPool>
    ) : ResponseObject() {

        override val group: Group = Group.System
        override val command: Command = Command.System.MemoryPoolStats

        data class MemoryPool(
            @JsonProperty("blksiz") val blockSize: Int,
            @JsonProperty("nblks") val blocks: Int,
            @JsonProperty("nfree") val freeBlocks: Int,
            @JsonProperty("min") val freeMinimum: Int
        )
    }

    /*
     * Read Datetime
     */

    object ReadDatetimeRequest : RequestObject() {

        override val operation: Operation = Operation.Read
        override val group: Group = Group.System
        override val command: Command = Command.System.Datetime
    }

    data class ReadDatetimeResponse(
        @JsonProperty("datetime") val datetime: String
    ) : ResponseObject() {

        override val group: Group = Group.System
        override val command: Command = Command.System.Datetime
    }

    /*
     * Write Datetime
     */

    data class WriteDatetimeRequest(
        @JsonProperty("datetime") val datetime: String
    ) : RequestObject() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.System
        override val command: Command = Command.System.Datetime
    }

    object WriteDatetimeResponse : ResponseObject() {

        override val group: Group = Group.System
        override val command: Command = Command.System.Datetime
    }

    /*
     * Reset
     */

    object ResetRequest : RequestObject() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.System
        override val command: Command = Command.System.Reset
    }

    object ResetResponse : ResponseObject() {

        override val group: Group = Group.System
        override val command: Command = Command.System.Reset
    }
}
