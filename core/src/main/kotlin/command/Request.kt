package com.juul.mcumgr.command

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

abstract class Request {
    abstract val operation: Operation
    abstract val group: Group
    abstract val command: Command
}

/**
 * Redeclaration of [Request] for serialization annotations.
 */
@JsonIgnoreProperties("operation", "group", "command")
internal abstract class RequestObject : Request()
