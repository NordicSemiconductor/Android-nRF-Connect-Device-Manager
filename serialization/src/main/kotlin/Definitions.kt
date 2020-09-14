package com.juul.mcumgr.serialization

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

private val readOnly: Set<Operation> = setOf(Operation.Read)
private val writeOnly: Set<Operation> = setOf(Operation.Write)
private val readWrite: Set<Operation> = setOf(Operation.Read, Operation.Write)

/**
 * The serialization object defining a request.
 */
@JsonIgnoreProperties("operation", "group", "command")
internal abstract class RequestDefinition {
    abstract val operation: Operation
    abstract val group: Group
    abstract val command: Command
}

/**
 * The serialization object defining a response.
 */
@JsonIgnoreProperties("group", "command", "accept")
internal abstract class ResponseDefinition {
    abstract val accept: Set<Operation>
    abstract val group: Group
    abstract val command: Command
}

/**
 * System group request and response definitions.
 */
internal sealed class System {

    /*
     * Echo
     */

    data class EchoRequest(
        @JsonProperty("d") val echo: String
    ) : RequestDefinition() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.System
        override val command: Command = Command.System.Echo
    }

    data class EchoResponse(
        @JsonProperty("r") val echo: String
    ) : ResponseDefinition() {

        override val accept: Set<Operation> = readWrite
        override val group: Group = Group.System
        override val command: Command = Command.System.Echo
    }

    /*
     * Console Echo Control
     */

    data class ConsoleEchoControlRequest(
        @JsonProperty("echo") val enabled: Boolean
    ) : RequestDefinition() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.System
        override val command: Command = Command.System.ConsoleEchoControl
    }

    object ConsoleEchoControlResponse : ResponseDefinition() {

        override val accept: Set<Operation> = writeOnly
        override val group: Group = Group.System
        override val command: Command = Command.System.ConsoleEchoControl
    }

    /*
     * Task Stats
     */

    object TaskStatsRequest : RequestDefinition() {

        override val operation: Operation = Operation.Read
        override val group: Group = Group.System
        override val command: Command = Command.System.TaskStats
    }

    data class TaskStatsResponse(
        @JsonProperty("tasks") val tasks: Map<String, Task>
    ) : ResponseDefinition() {

        override val accept: Set<Operation> = readOnly
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

    object MemoryPoolStatsRequest : RequestDefinition() {

        override val operation: Operation = Operation.Read
        override val group: Group = Group.System
        override val command: Command = Command.System.MemoryPoolStats
    }

    data class MemoryPoolStatsResponse(
        @JsonProperty("mpools") val memoryPools: Map<String, MemoryPool>
    ) : ResponseDefinition() {

        override val accept: Set<Operation> = readOnly
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

    object ReadDatetimeRequest : RequestDefinition() {

        override val operation: Operation = Operation.Read
        override val group: Group = Group.System
        override val command: Command = Command.System.Datetime
    }

    data class ReadDatetimeResponse(
        @JsonProperty("datetime") val datetime: String
    ) : ResponseDefinition() {

        override val accept: Set<Operation> = readOnly
        override val group: Group = Group.System
        override val command: Command = Command.System.Datetime
    }

    /*
     * Write Datetime
     */

    data class WriteDatetimeRequest(
        @JsonProperty("datetime") val datetime: String
    ) : RequestDefinition() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.System
        override val command: Command = Command.System.Datetime
    }

    object WriteDatetimeResponse : ResponseDefinition() {

        override val accept: Set<Operation> = writeOnly
        override val group: Group = Group.System
        override val command: Command = Command.System.Datetime
    }

    /*
     * Reset
     */

    object ResetRequest : RequestDefinition() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.System
        override val command: Command = Command.System.Reset
    }

    object ResetResponse : ResponseDefinition() {

        override val accept: Set<Operation> = writeOnly
        override val group: Group = Group.System
        override val command: Command = Command.System.Reset
    }
}

/**
 * Image group request and response definitions.
 */
internal sealed class Image {

    /*
     * Image Write
     */

    data class ImageWriteRequest(
        @JsonProperty("data") val data: ByteArray,
        @JsonProperty("off") val offset: Int,
        @JsonProperty("len") val size: Int? = null,
        @JsonProperty("sha") val hash: ByteArray? = null
    ) : RequestDefinition() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.Image
        override val command: Command = Command.Image.Upload
    }

    data class ImageWriteResponse(
        @JsonProperty("off") val offset: Int
    ) : ResponseDefinition() {

        override val accept: Set<Operation> = writeOnly
        override val group: Group = Group.Image
        override val command: Command = Command.Image.Upload
    }


    /*
     * Core Read
     */

    data class CoreReadRequest(
        @JsonProperty("off") val offset: Int
    ) : RequestDefinition() {

        override val operation: Operation = Operation.Read
        override val group: Group = Group.Image
        override val command: Command = Command.Image.CoreDownload
    }

    data class CoreReadResponse(
        @JsonProperty("data") val data: ByteArray,
        @JsonProperty("off") val offset: Int,
        @JsonProperty("len") val length: Int? = null
    ) : ResponseDefinition() {

        override val accept: Set<Operation> = readOnly
        override val group: Group = Group.Image
        override val command: Command = Command.Image.CoreDownload
    }

}

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
    ) : RequestDefinition() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.File
        override val command: Command = Command.File.File
    }

    data class WriteResponse(
        @JsonProperty("off") val offset: Int
    ) : ResponseDefinition() {

        override val accept: Set<Operation> = writeOnly
        override val group: Group = Group.File
        override val command: Command = Command.File.File
    }

    /*
     * File Read
     */

    data class ReadRequest(
        @JsonProperty("name") val fileName: String,
        @JsonProperty("off") val offset: Int
    ) : RequestDefinition() {

        override val operation: Operation = Operation.Read
        override val group: Group = Group.File
        override val command: Command = Command.File.File
    }

    data class ReadResponse(
        @JsonProperty("data") val data: ByteArray,
        @JsonProperty("off") val offset: Int,
        @JsonProperty("len") val length: Int? = null
    ) : ResponseDefinition() {

        override val accept: Set<Operation> = readOnly
        override val group: Group = Group.File
        override val command: Command = Command.File.File
    }
}



