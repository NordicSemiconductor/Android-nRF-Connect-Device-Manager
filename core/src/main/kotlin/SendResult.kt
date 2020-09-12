package com.juul.mcumgr

import com.juul.mcumgr.SendResult.Failure
import com.juul.mcumgr.message.ResponseCode

sealed class SendResult<out T> {

    data class Response<out T>(
        val body: T?,
        val code: ResponseCode
    ) : SendResult<T>()

    data class Failure<out T>(
        val throwable: Throwable
    ) : SendResult<T>()

    val isSuccess: Boolean
        get() = this is Response && code.isSuccess

    val isError: Boolean
        get() = this is Response && code.isError

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
        is SendResult.Response -> body ?: throw ErrorCodeException(code)
        is Failure -> throw throwable
    }
}

inline fun <T> SendResult<T>.getOrElse(action: (exception: Throwable) -> T): T {
    return when (this) {
        is SendResult.Response -> body ?: action(ErrorCodeException(code))
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
                action(ErrorCodeException(code))
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
                Failure(ErrorCodeException(code))
            }
        }
        is Failure<T> -> Failure(throwable)
    }
}

class ErrorCodeException(
    val code: ResponseCode
) : IllegalStateException("Request resulted in error response $code")
