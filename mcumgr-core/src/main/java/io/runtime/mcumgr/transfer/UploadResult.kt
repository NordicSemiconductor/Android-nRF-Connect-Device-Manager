package io.runtime.mcumgr.transfer

import io.runtime.mcumgr.McuMgrErrorCode
import io.runtime.mcumgr.R
import io.runtime.mcumgr.response.UploadResponse

class ErrorResponseException internal constructor(
    val code: McuMgrErrorCode
) : IllegalStateException("Request resulted in error response $code")

internal sealed class UploadResult {

    data class Response(
        val body: UploadResponse,
        val code: McuMgrErrorCode
    ) : UploadResult()

    data class Failure(
        val throwable: Throwable
    ) : UploadResult()

    val isSuccess: Boolean
        get() = this is Response && code.isSuccess

    val isError: Boolean
        get() = this is Response && code.isError

    val isFailure: Boolean
        get() = this is Failure
}

internal inline fun UploadResult.onSuccess(
    action: (response: UploadResponse) -> Unit
): UploadResult {
    when (this) {
        is UploadResult.Response -> {
            if (code.isSuccess) {
                action(body)
            }
        }
    }
    return this
}

internal inline fun UploadResult.onErrorOrFailure(action: (throwable: Throwable) -> Unit): UploadResult {
    when (this) {
        is UploadResult.Response -> {
            if (code.isError) {
                action(ErrorResponseException(code))
            }
        }
        is UploadResult.Failure -> action(throwable)
    }
    return this
}

internal inline fun UploadResult.mapResponse(transform: (result: UploadResult.Response) -> UploadResult): UploadResult {
    return when (this) {
        is UploadResult.Response -> transform(this)
        else -> this
    }
}

internal val McuMgrErrorCode.isSuccess: Boolean
    get() = this == McuMgrErrorCode.OK

internal val McuMgrErrorCode.isError: Boolean
    get() = this != McuMgrErrorCode.OK
