package com.juul.mcumgr.message

abstract class Response {

    sealed class Code(val value: Int) {

        object Ok : Code(0)
        object Unknown : Code(1)
        object NoMemory : Code(2)
        object InValue : Code(3)
        object Timeout : Code(4)
        object NoEntry : Code(5)
        object BadState : Code(6)
        object TooLarge : Code(7)
        object NotSupported : Code(8)

        companion object {
            @JvmStatic
            fun valueOf(value: Int): Code? = when (value) {
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
}
