package com.juul.mcumgr

import com.juul.mcumgr.SendResult.Failure
import com.juul.mcumgr.command.ResponseCode

class ErrorResponseException(
    val code: ResponseCode
) : IllegalStateException("Request resulted in error response $code")

/**
 * Result of sending a request over the [Transport], either a [Response] or a [Failure].
 *
 * [Response] indicates that the request produced an mcumgr response, which itself may be an error.
 * [Failure] indicates that the request either failed to send, or failed to produce a response.
 */
sealed class SendResult<out T> {

    /**
     * Indicates that the request produced an mcumgr response, which itself may be an
     * error. Generally, a non-zero [ResponseCode] indicates that the response is an error. If the
     * response body failed to be decoded, but the response contained a valid "rc" field (code),
     * then the [Response.body] field will be null.
     */
    data class Response<out T>(
        val body: T?,
        val code: ResponseCode
    ) : SendResult<T>()

    /**
     * Indicates that the request either failed to send, or failed to produce a response. These
     * failures may be specific to the transport implementation.
     */
    data class Failure<out T>(
        val throwable: Throwable
    ) : SendResult<T>()

    /**
     * A response has been received with a non-null body and code [ResponseCode.Ok].
     */
    val isSuccess: Boolean
        get() = this is Response && body != null && code.isSuccess

    /**
     * A response has been received win a non-zero code.
     */
    val isError: Boolean
        get() = this is Response && code.isError

    /**
     * The result is a failure.
     */
    val isFailure: Boolean
        get() = this is Failure

    fun exceptionOrNull(): Throwable? =
        when (this) {
            is Failure -> throwable
            else -> null
        }
}

inline fun <T> SendResult<T>.onSuccess(
    action: (response: T) -> Unit
): SendResult<T> {
    when (this) {
        is SendResult.Response -> {
            if (body != null && code.isSuccess) {
                action(body)
            }
        }
    }
    return this
}

inline fun SendResult<*>.onFailure(action: (throwable: Throwable) -> Unit): SendResult<*> {
    when (this) {
        is Failure -> action(throwable)
    }
    return this
}

fun <T> SendResult<T>.getOrThrow(): T {
    return when (this) {
        is SendResult.Response -> body ?: throw ErrorResponseException(code)
        is Failure -> throw throwable
    }
}

inline fun <T> SendResult<T>.getOrElse(action: (exception: Throwable) -> T): T {
    return when (this) {
        is SendResult.Response -> body ?: action(ErrorResponseException(code))
        is Failure -> action(throwable)
    }
}

/*
 * Internal extension functions. Useful for more advanced response and error handling.
 */

internal inline fun <T> SendResult<T>.onResponse(
    action: (SendResult.Response<T>) -> Unit
): SendResult<T> {
    when (this) {
        is SendResult.Response -> action(this)
    }
    return this
}

internal inline fun <T> SendResult<T>.onResponseCode(
    vararg codes: ResponseCode,
    action: (response: SendResult.Response<T>) -> Unit
): SendResult<T> {
    when (this) {
        is SendResult.Response -> {
            if (codes.contains(code)) {
                action(this)
            }
        }
    }
    return this
}

internal inline fun SendResult<*>.onErrorOrFailure(action: (throwable: Throwable) -> Unit): SendResult<*> {
    when (this) {
        is SendResult.Response -> {
            if (code.isError) {
                action(ErrorResponseException(code))
            }
        }
        is Failure -> action(throwable)
    }
    return this
}

internal inline fun <T, R> SendResult<T>.mapResponse(transform: (response: T) -> R): SendResult<R> {
    return when (this) {
        is SendResult.Response -> {
            if (body != null) {
                SendResult.Response(transform(body), code)
            } else {
                Failure(ErrorResponseException(code))
            }
        }
        is Failure<T> -> Failure(throwable)
    }
}
