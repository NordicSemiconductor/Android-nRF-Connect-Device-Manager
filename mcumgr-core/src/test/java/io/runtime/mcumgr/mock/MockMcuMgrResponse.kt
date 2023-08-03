package io.runtime.mcumgr.mock

import io.runtime.mcumgr.McuMgrErrorCode
import io.runtime.mcumgr.McuMgrHeader
import io.runtime.mcumgr.McuMgrScheme
import io.runtime.mcumgr.response.McuMgrResponse
import io.runtime.mcumgr.util.CBOR

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
