package no.nordicsemi.android.mcumgr.mock

import no.nordicsemi.android.mcumgr.McuMgrHeader
import no.nordicsemi.android.mcumgr.response.McuMgrResponse

interface McuMgrHandler {
    fun <T: McuMgrResponse> handle(
        header: McuMgrHeader,
        payload: ByteArray,
        responseType: Class<T>
    ): T
}

interface OverrideHandler: McuMgrHandler {
    val groupId: Int
    val commandId: Int
}
