package com.juul.mcumgr

import com.juul.mcumgr.McuMgrResult.Error
import com.juul.mcumgr.McuMgrResult.Failure
import com.juul.mcumgr.McuMgrResult.Success
import com.juul.mcumgr.message.Response

sealed class McuMgrResult<out T> {

    data class Success<T>(
        val value: T,
        val code: Response.Code = Response.Code.Ok
    ) : McuMgrResult<T>()

    data class Error<T>(val code: Response.Code) : McuMgrResult<T>()

    data class Failure<T>(val throwable: Throwable) : McuMgrResult<T>()

    val isSuccess: Boolean
        get() = this is Success

    val isError: Boolean
        get() = this is Error

    val isFailure: Boolean
        get() = this is Failure

    fun exceptionOrNull(): Throwable? =
        when (this) {
            is Failure -> throwable
            else -> null
        }
}

inline fun <T> McuMgrResult<T>.onSuccess(action: (response: T) -> Unit): McuMgrResult<T> {
    when (this) {
        is Success -> action(value)
    }
    return this
}

inline fun McuMgrResult<*>.onError(action: (code: Response.Code) -> Unit): McuMgrResult<*> {
    when (this) {
        is Error -> action(code)
    }
    return this
}

inline fun McuMgrResult<*>.onFailure(action: (throwable: Throwable) -> Unit): McuMgrResult<*> {
    when (this) {
        is Failure -> action(throwable)
    }
    return this
}

inline fun McuMgrResult<*>.onErrorOrFailure(action: (throwable: Throwable) -> Unit): McuMgrResult<*> {
    when (this) {
        is Error -> action(ErrorCodeException(code))
        is Failure -> action(throwable)
    }
    return this
}

inline fun <T> McuMgrResult<T>.getOrThrow(): T {
    return when (this) {
        is Success -> value
        is Error -> throw ErrorCodeException(code)
        is Failure -> throw throwable
    }
}

inline fun <T> McuMgrResult<T>.getOrElse(action: (exception: Throwable) -> T): T {
    return when (this) {
        is Success -> value
        is Error -> action(ErrorCodeException(code))
        is Failure -> action(throwable)
    }
}

inline fun <R, T> McuMgrResult<T>.map(transform: (value: T) -> R): McuMgrResult<R> {
    return when (this) {
        is Success -> Success(transform(value))
        is Error -> Error(code)
        is Failure -> Failure(throwable)
    }
}

/**
 * Calls the specified function [block] and returns its encapsulated result if invocation was successful,
 * catching any [Throwable] exception that was thrown from the [block] function execution and encapsulating it as a failure.
 */
inline fun <R> catchResult(block: () -> R): McuMgrResult<R> {
    return try {
        Success(block())
    } catch (e: ErrorCodeException) {
        Error(e.code)
    } catch (e: Throwable) {
        Failure(e)
    }
}

/**
 * Calls the specified function [block] with `this` value as its receiver and returns its encapsulated result if invocation was successful,
 * catching any [Throwable] exception that was thrown from the [block] function execution and encapsulating it as a failure.
 */
inline fun <T, R> T.catchResult(block: T.() -> R): McuMgrResult<R> {
    return try {
        Success(block())
    } catch (e: ErrorCodeException) {
        Error(e.code)
    } catch (e: Throwable) {
        Failure(e)
    }
}

class ErrorCodeException(
    val code: Response.Code
) : IllegalStateException("Request resulted in error response $code")
