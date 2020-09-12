package com.juul.mcumgr.serialization

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

val readOnly: Set<Operation> = setOf(Operation.Read)
val writeOnly: Set<Operation> = setOf(Operation.Write)
val readWrite: Set<Operation> = setOf(Operation.Read, Operation.Write)

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

    // TODO

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

    // TODO

    /*
     * Reset
     */

    // TODO
}

/**
 * Image group request and response definitions.
 */
sealed class Image {

    /*
     * Image Write
     */

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
    ) : ResponseDefinition() {

        override val accept: Set<Operation> = writeOnly
        override val group: Group = Group.Image
        override val command: Command = Command.Image.Upload
    }


    /*
     * Core Read
     */

    internal data class CoreReadRequest(
        @JsonProperty("off") val offset: Int
    ) : RequestDefinition() {

        override val operation: Operation = Operation.Read
        override val group: Group = Group.Image
        override val command: Command = Command.Image.CoreDownload
    }

    internal data class CoreReadResponse(
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

    internal data class WriteRequest(
        @JsonProperty("name") val fileName: String,
        @JsonProperty("data") val data: ByteArray,
        @JsonProperty("off") val offset: Int,
        @JsonProperty("len") val length: Int? = null
    ) : RequestDefinition() {

        override val operation: Operation = Operation.Write
        override val group: Group = Group.File
        override val command: Command = Command.File.File
    }

    internal data class WriteResponse(
        @JsonProperty("off") val offset: Int
    ) : ResponseDefinition() {

        override val accept: Set<Operation> = writeOnly
        override val group: Group = Group.File
        override val command: Command = Command.File.File
    }

    /*
     * File Read
     */

    internal data class ReadRequest(
        @JsonProperty("name") val fileName: String,
        @JsonProperty("off") val offset: Int
    ) : RequestDefinition() {

        override val operation: Operation = Operation.Read
        override val group: Group = Group.File
        override val command: Command = Command.File.File
    }

    internal data class ReadResponse(
        @JsonProperty("data") val data: ByteArray,
        @JsonProperty("off") val offset: Int,
        @JsonProperty("len") val length: Int? = null
    ) : ResponseDefinition() {

        override val accept: Set<Operation> = readOnly
        override val group: Group = Group.File
        override val command: Command = Command.File.File
    }
}



