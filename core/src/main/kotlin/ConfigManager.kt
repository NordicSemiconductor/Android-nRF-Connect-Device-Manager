package com.juul.mcumgr

import com.juul.mcumgr.command.CommandResult
import com.juul.mcumgr.command.Config
import com.juul.mcumgr.command.mapResponse
import com.juul.mcumgr.command.toUnitResult

class ConfigManager internal constructor(val transport: Transport) {

    suspend fun read(name: String): CommandResult<ConfigReadResponse> {
        val request = Config.ReadReqeust(name)
        return transport.send(
            request,
            Config.ReadResponse::class
        ).mapResponse { response ->
            ConfigReadResponse(response.value)
        }
    }

    suspend fun write(
        name: String,
        value: String,
        save: Boolean = true
    ): CommandResult<Unit> {
        val request = Config.WriteRequest(name, value, save)
        return transport.send(
            request,
            Config.WriteResponse::class
        ).toUnitResult()
    }
}

data class ConfigReadResponse(
    val value: String
)
