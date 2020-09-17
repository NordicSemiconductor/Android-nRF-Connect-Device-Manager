package com.juul.mcumgr.command

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

abstract class Response {
    abstract val group: Group
    abstract val command: Command
}

/**
 * Redeclaration of [Response] for serialization annotations.
 */
@JsonIgnoreProperties("group", "command")
internal abstract class ResponseObject : Response()
