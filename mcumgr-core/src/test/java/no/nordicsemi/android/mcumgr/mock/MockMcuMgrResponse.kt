package no.nordicsemi.android.mcumgr.mock

import no.nordicsemi.android.mcumgr.McuMgrErrorCode
import no.nordicsemi.android.mcumgr.McuMgrHeader
import no.nordicsemi.android.mcumgr.McuMgrScheme
import no.nordicsemi.android.mcumgr.response.McuMgrResponse
import no.nordicsemi.android.mcumgr.util.CBOR

/**
 * Build a mock error response.
 */
fun <T: McuMgrResponse?> buildMockErrorResponse(
    scheme: McuMgrScheme,
    errorCode: McuMgrErrorCode,
    responseHeader: McuMgrHeader,
    responseType: Class<T>,
    codeClass: Int = 2,
    codeDetail: Int = 5
): T {
    val responsePayload = CBOR.toBytes(McuMgrErrorResponse(errorCode))
    when (scheme) {
        McuMgrScheme.BLE ->
            return McuMgrResponse.buildResponse(
                McuMgrScheme.BLE,
                responsePayload,
                responseType
            )
        McuMgrScheme.COAP_BLE ->
            return McuMgrResponse.buildCoapResponse(
                McuMgrScheme.COAP_BLE,
                responsePayload,
                responseHeader.toBytes(),
                responsePayload,
                codeClass,
                codeDetail,
                responseType
            )
        else -> {
            throw IllegalArgumentException("Unsupported scheme: $scheme")
        }
    }
}

/**
 * Build a mock response.
 */
fun <T: McuMgrResponse> buildMockResponse(
    scheme: McuMgrScheme,
    responseHeader: McuMgrHeader,
    responsePayload: ByteArray,
    responseType: Class<T>,
    codeClass: Int = 2,
    codeDetail: Int = 5
): T = when (scheme) {
    McuMgrScheme.BLE ->
        McuMgrResponse.buildResponse(
            McuMgrScheme.BLE,
            responseHeader.toBytes() + responsePayload,
            responseType
        )
    McuMgrScheme.COAP_BLE ->
        McuMgrResponse.buildCoapResponse(
            McuMgrScheme.COAP_BLE,
            responsePayload,
            responseHeader.toBytes(),
            responsePayload,
            codeClass,
            codeDetail,
            responseType
        )
    else -> {
        throw IllegalArgumentException("Unsupported scheme: $scheme")
    }
}

/**
 * Helper class for building an mcumgr error response.
 */
class McuMgrErrorResponse(errorCode: McuMgrErrorCode): McuMgrResponse() {
    init {
        rc = errorCode.value()
    }
}

/**
 * Return a new mcumgr header with the operation converted to a response.
 */
fun McuMgrHeader.toResponse(): McuMgrHeader {
    val newOp = when (op) {
        McuMgrOperation.READ.value -> McuMgrOperation.READ_RESPONSE.value
        McuMgrOperation.WRITE.value -> McuMgrOperation.WRITE_RESPONSE.value
        else -> op
    }
    return McuMgrHeader(version, newOp, flags, len, groupId, sequenceNum, commandId)
}
