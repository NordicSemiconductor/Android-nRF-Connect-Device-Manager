package com.juul.mcumgr.serialization

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.juul.mcumgr.message.Command
import com.juul.mcumgr.message.Group
import com.juul.mcumgr.message.Operation

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
internal abstract class ResponseDefinition

/**
 * System group request and response definitions.
 */
internal sealed class System {

    data class EchoRequest(
        @JsonProperty("d") val echo: String
    ) : RequestDefinition() {
        override val operation: Operation = Operation.Write
        override val group: Group = Group.System
        override val command: Command = Command.System.Echo
    }

    data class EchoResponse(
        @JsonProperty("r") val echo: String
    ) : ResponseDefinition()

    object TaskStatsRequest : RequestDefinition() {
        override val operation: Operation = Operation.Read
        override val group: Group = Group.System
        override val command: Command = Command.System.TaskStats
    }

    data class TaskStatsResponse(
        @JsonProperty("tasks") val tasks: Map<String, Task>
    ) : ResponseDefinition() {

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
}

/**
 * Image group request and response definitions.
 */
sealed class Image {

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
    ) : ResponseDefinition()

    internal data class CoreReadRequest(
        @JsonProperty("off") val offset: Int
    ) : RequestDefinition() {
        override val operation: Operation = Operation.Read
        override val group: Group = Group.Image
        override val command: Command = Command.Image.CoreLoad
    }

    internal data class CoreReadResponse(
        @JsonProperty("data") val data: ByteArray,
        @JsonProperty("off") val offset: Int,
        @JsonProperty("len") val length: Int? = null
    ) : ResponseDefinition()
}

/**
 * File group request and response definitions.
 */
internal sealed class File {

    internal data class WriteRequest(
        @JsonProperty("name") val fileName: String,
        @JsonProperty("data") val data: ByteArray,
        @JsonProperty("off") val offset: Int,
        @JsonProperty("len") val length: Int? = null
    ) : RequestDefinition() {
        override val operation: Operation = Operation.Write
        override val group: Group = Group.File
        override val command: Command = Command.Files.File
    }

    internal data class WriteResponse(
        @JsonProperty("off") val offset: Int
    ) : ResponseDefinition()

    internal data class ReadRequest(
        @JsonProperty("name") val fileName: String,
        @JsonProperty("off") val offset: Int
    ) : RequestDefinition() {
        override val operation: Operation = Operation.Read
        override val group: Group = Group.File
        override val command: Command = Command.Files.File
    }

    internal data class ReadResponse(
        @JsonProperty("data") val data: ByteArray,
        @JsonProperty("off") val offset: Int,
        @JsonProperty("len") val length: Int? = null
    ) : ResponseDefinition()
}



