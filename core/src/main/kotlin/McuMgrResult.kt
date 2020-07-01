package com.juul.mcumgr

sealed class McuMgrResult<out T> {

    val isSuccess: Boolean
        get() = this is Success

    val isFailure: Boolean
        get() = this is Failure

    fun exceptionOrNull(): Throwable? =
        when (this) {
            is Failure -> throwable
            else -> null
        }
}

// Result Types

data class Success<T>(val value: T) : McuMgrResult<T>()

data class Failure<T>(val throwable: Throwable) : McuMgrResult<T>()

// On Blocks

inline fun <T> McuMgrResult<T>.onSuccess(action: (response: T) -> Unit): McuMgrResult<T> {
    when (this) {
        is Success -> action(value)
    }
    return this
}

inline fun McuMgrResult<*>.onFailure(action: (throwable: Throwable) -> Unit): McuMgrResult<*> {
    when (this) {
        is Failure -> action(throwable)
    }
    return this
}

// Response Or

inline fun <T> McuMgrResult<T>.getOrThrow(): T {
    return when (this) {
        is Success -> value
        is Failure -> throw throwable
    }
}

inline fun <T> McuMgrResult<T>.getOrElse(onFailure: (exception: Throwable) -> T): T {
    return when (this) {
        is Success -> value
        is Failure -> onFailure(throwable)
    }
}

// Transformations

inline fun <R, T> McuMgrResult<T>.map(transform: (value: T) -> R): McuMgrResult<R> {
    return when (this) {
        is Success -> Success(transform(value))
        is Failure -> Failure(throwable)
    }
}
