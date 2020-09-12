package com.juul.mcumgr

import com.juul.mcumgr.message.ConsoleEchoControlRequest
import com.juul.mcumgr.message.ConsoleEchoControlResponse
import com.juul.mcumgr.message.EchoRequest
import com.juul.mcumgr.message.EchoResponse
import com.juul.mcumgr.message.MemoryPoolStatsRequest
import com.juul.mcumgr.message.MemoryPoolStatsResponse
import com.juul.mcumgr.message.TaskStatsRequest
import com.juul.mcumgr.message.TaskStatsResponse

class SystemManager(val transport: Transport) {

    suspend fun echo(echo: String): SendResult<EchoResponse> =
        transport.send(EchoRequest(echo))

    suspend fun consoleEchoControl(
        enabled: Boolean
    ): SendResult<ConsoleEchoControlResponse> =
        transport.send(ConsoleEchoControlRequest(enabled))

    suspend fun taskStats(): SendResult<TaskStatsResponse> =
        transport.send(TaskStatsRequest)

    suspend fun memoryPoolStats(): SendResult<MemoryPoolStatsResponse> =
        transport.send(MemoryPoolStatsRequest)
}
