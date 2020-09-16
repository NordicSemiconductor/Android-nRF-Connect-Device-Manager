package com.juul.mcumgr.command

import com.fasterxml.jackson.annotation.JsonProperty
import com.juul.mcumgr.Command
import com.juul.mcumgr.Group
import com.juul.mcumgr.Operation

/**
 * System group request and response definitions.
 */
sealed class System {

    /*
     * Echo
     */

    data class EchoRequest(
        @JsonProperty("d") val echo: String
    ) : Request() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.System
        override val command: Command = Command.System.Echo
    }

    data class EchoResponse(
        @JsonProperty("r") val echo: String
    ) : Response() {

        override val group: Group = Group.System
        override val command: Command = Command.System.Echo
    }

    /*
     * Console Echo Control
     */

    data class ConsoleEchoControlRequest(
        @JsonProperty("echo") val enabled: Boolean
    ) : Request() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.System
        override val command: Command = Command.System.ConsoleEchoControl
    }

    object ConsoleEchoControlResponse : Response() {

        override val group: Group = Group.System
        override val command: Command = Command.System.ConsoleEchoControl
    }

    /*
     * Task Stats
     */

    object TaskStatsRequest : Request() {

        override val operation: Operation = Operation.Read
        override val group: Group = Group.System
        override val command: Command = Command.System.TaskStats
    }

    data class TaskStatsResponse(
        @JsonProperty("tasks") val tasks: Map<String, Task>
    ) : Response() {

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

    object MemoryPoolStatsRequest : Request() {

        override val operation: Operation = Operation.Read
        override val group: Group = Group.System
        override val command: Command = Command.System.MemoryPoolStats
    }

    data class MemoryPoolStatsResponse(
        @JsonProperty("mpools") val memoryPools: Map<String, MemoryPool>
    ) : Response() {

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

    object ReadDatetimeRequest : Request() {

        override val operation: Operation = Operation.Read
        override val group: Group = Group.System
        override val command: Command = Command.System.Datetime
    }

    data class ReadDatetimeResponse(
        @JsonProperty("datetime") val datetime: String
    ) : Response() {

        override val group: Group = Group.System
        override val command: Command = Command.System.Datetime
    }

    /*
     * Write Datetime
     */

    data class WriteDatetimeRequest(
        @JsonProperty("datetime") val datetime: String
    ) : Request() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.System
        override val command: Command = Command.System.Datetime
    }

    object WriteDatetimeResponse : Response() {

        override val group: Group = Group.System
        override val command: Command = Command.System.Datetime
    }

    /*
     * Reset
     */

    object ResetRequest : Request() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.System
        override val command: Command = Command.System.Reset
    }

    object ResetResponse : Response() {

        override val group: Group = Group.System
        override val command: Command = Command.System.Reset
    }
}
