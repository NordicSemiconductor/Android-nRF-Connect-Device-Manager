package com.juul.mcumgr.message

sealed class ResponseCode(val value: Int) {

    object Ok : ResponseCode(0)
    object Unknown : ResponseCode(1)
    object NoMemory : ResponseCode(2)
    object InValue : ResponseCode(3)
    object Timeout : ResponseCode(4)
    object NoEntry : ResponseCode(5)
    object BadState : ResponseCode(6)
    object TooLarge : ResponseCode(7)
    object NotSupported : ResponseCode(8)

    companion object {
        @JvmStatic
        fun valueOf(value: Int): ResponseCode? = when (value) {
            0 -> Ok
            1 -> Unknown
            2 -> NoMemory
            3 -> InValue
            4 -> Timeout
            5 -> NoEntry
            6 -> BadState
            7 -> TooLarge
            8 -> NotSupported
            else -> null
        }
    }

    val isSuccess: Boolean get() = this is Ok

    val isError: Boolean get() = !isSuccess
}
