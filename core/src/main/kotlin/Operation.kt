package com.juul.mcumgr

sealed class Operation(val value: Int) {
    object Read : Operation(0)
    object ReadResponse : Operation(1)
    object Write : Operation(2)
    object WriteResponse : Operation(3)
}
