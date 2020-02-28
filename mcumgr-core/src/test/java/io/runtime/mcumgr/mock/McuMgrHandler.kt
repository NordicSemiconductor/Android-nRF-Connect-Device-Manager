package io.runtime.mcumgr.mock

import io.runtime.mcumgr.McuMgrHeader
import io.runtime.mcumgr.response.McuMgrResponse

interface McuMgrHandler {
    fun <T: McuMgrResponse?> handle(
        header: McuMgrHeader,
        payload: ByteArray,
        responseType: Class<T>
    ): T
}

interface OverrideHandler: McuMgrHandler {
    val groupId: Int
    val commandId: Int
}
