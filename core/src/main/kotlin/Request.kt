package com.juul.mcumgr

abstract class Request {
    abstract val operation: Operation
    abstract val group: Group
    abstract val command: Command
}
