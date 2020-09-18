package com.juul.mcumgr

import com.juul.mcumgr.command.CommandResult
import com.juul.mcumgr.command.Request
import com.juul.mcumgr.command.Response
import kotlin.reflect.KClass

enum class Protocol {
    SMP, OMP
}

interface Transport {

    // TODO both of these are only used in `Uploader`. Maybe remove these to inject elsewhere?
    val mtu: Int
    val protocol: Protocol

    suspend fun <T : Response> send(request: Request, responseType: KClass<T>): CommandResult<T>
}
