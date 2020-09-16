package com.juul.mcumgr.util

import com.juul.mcumgr.CommandResult
import com.juul.mcumgr.ErrorResponseException
import com.juul.mcumgr.ResponseCode

/*
 * Result extension functions. Useful for more advanced response and error handling.
 */

internal inline fun <T> CommandResult<T>.onResponse(
    action: (CommandResult.Response<T>) -> Unit
): CommandResult<T> {
    when (this) {
        is CommandResult.Response -> action(this)
    }
    return this
}

internal inline fun <T> CommandResult<T>.onResponseCode(
    vararg codes: ResponseCode,
    action: (response: CommandResult.Response<T>) -> Unit
): CommandResult<T> {
    when (this) {
        is CommandResult.Response -> {
            if (codes.contains(code)) {
                action(this)
            }
        }
    }
    return this
}

internal inline fun CommandResult<*>.onErrorOrFailure(action: (throwable: Throwable) -> Unit): CommandResult<*> {
    when (this) {
        is CommandResult.Response -> {
            if (code.isError) {
                action(ErrorResponseException(code))
            }
        }
        is CommandResult.Failure -> action(throwable)
    }
    return this
}

internal inline fun <T, R> CommandResult<T>.mapResponse(transform: (response: T) -> R): CommandResult<R> {
    return when (this) {
        is CommandResult.Response -> {
            // Smart cast to 'T' is impossible, because 'body' is a public API
            // property declared in different module
            val body = body
            if (body != null) {
                CommandResult.Response(transform(body), code)
            } else {
                CommandResult.Response(null, code)
            }
        }
        is CommandResult.Failure<T> -> CommandResult.Failure(throwable)
    }
}

internal fun CommandResult<*>.toUnitResult(): CommandResult<Unit> {
    return when (this) {
        is CommandResult.Response<*> -> CommandResult.Response(Unit, code)
        is CommandResult.Failure<*> -> CommandResult.Failure(throwable)
    }
}
