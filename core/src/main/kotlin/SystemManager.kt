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

    suspend fun echo(echo: String): McuMgrResult<EchoResponse> =
        transport.send(EchoRequest(echo))

    suspend fun consoleEchoControl(
        enabled: Boolean
    ): McuMgrResult<ConsoleEchoControlResponse> =
        transport.send(ConsoleEchoControlRequest(enabled))

    suspend fun taskStats(): McuMgrResult<TaskStatsResponse> =
        transport.send(TaskStatsRequest)

    suspend fun memoryPoolStats(): McuMgrResult<MemoryPoolStatsResponse> =
        transport.send(MemoryPoolStatsRequest)
}
