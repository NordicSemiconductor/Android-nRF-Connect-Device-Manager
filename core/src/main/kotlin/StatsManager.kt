package com.juul.mcumgr

import com.juul.mcumgr.command.CommandResult
import com.juul.mcumgr.command.Stats
import com.juul.mcumgr.command.mapResponse

class StatsManager internal constructor(val transport: Transport) {

    suspend fun read(name: String): CommandResult<StatsResponse> {
        val request = Stats.ReadRequest(name)
        return transport.send(
            request,
            Stats.ReadResponse::class
        ).mapResponse { response ->
            StatsResponse(response.name, response.fields)
        }
    }

    suspend fun list(): CommandResult<StatsListResponse> {
        val request = Stats.ListRequest
        return transport.send(
            request,
            Stats.ListResponse::class
        ).mapResponse { response ->
            StatsListResponse(response.groups)
        }
    }
}

data class StatsResponse(
    val name: String,
    val fields: Map<String, Long>
)

data class StatsListResponse(
    val groups: List<String>
)
