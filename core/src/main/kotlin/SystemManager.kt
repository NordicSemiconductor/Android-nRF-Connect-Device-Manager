package com.juul.mcumgr

import com.juul.mcumgr.message.Request
import com.juul.mcumgr.message.Response

data class EchoRequest(
    val echo: String
) : Request()

data class EchoResponse(
    val echo: String
) : Response()

data class ConsoleEchoControlRequest(
    val enabled: Boolean
) : Request()

object ConsoleEchoControlResponse : Response()

object TaskStatsRequest : Request()

data class TaskStatsResponse(
    val tasks: Map<String, Task>
) : Response() {

    data class Task(
        val priority: Int,
        val taskId: Int,
        val state: State,
        val stackUse: Int,
        val stackSize: Int,
        val contextSwitchCount: Int,
        val runtime: Int,
        val lastCheckIn: Int,
        val nextCheckIn: Int
    ) {

        sealed class State(value: Int) {

            sealed class Mynewt {
                object Ready : State(1)
                object Sleep : State(2)
            }

            class Zephyr(value: Int) : State(value) {
                val isDummy: Boolean = value and (1 shl 0) != 0
                val isPending: Boolean = value and (1 shl 1) != 0
                val isPrestart: Boolean = value and (1 shl 2) != 0
                val isDead: Boolean = value and (1 shl 3) != 0
                val isSuspended: Boolean = value and (1 shl 4) != 0
                val isQueued: Boolean = value and (1 shl 5) != 0
            }
        }
    }
}

/**
 * Manager
 */
class SystemManager(val transport: Transport) {

    suspend fun echo(echo: String): McuMgrResult<EchoResponse> =
        transport.send(EchoRequest(echo))

    suspend fun consoleEchoControl(
        enabled: Boolean
    ): McuMgrResult<ConsoleEchoControlResponse> =
        transport.send(ConsoleEchoControlRequest(enabled))

    suspend fun taskStats(): McuMgrResult<TaskStatsResponse> =
        transport.send(TaskStatsRequest)
}
