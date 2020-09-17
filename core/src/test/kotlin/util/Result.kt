package util

import com.juul.mcumgr.command.CommandResult
import com.juul.mcumgr.command.ResponseCode
import kotlin.test.assertEquals
import kotlin.test.fail

inline fun <T> CommandResult<T>.getOrAssert(): T {
    return when (this) {
        is CommandResult.Response -> body ?: fail("response body is null $this")
        is CommandResult.Failure -> fail("command resulted in failure $this")
    }
}


inline fun <T> CommandResult<T>.assertResponseCode(code: ResponseCode): CommandResult<T> {
    return when (this) {
        is CommandResult.Response -> {
            assertEquals(code, this.code)
            this
        }
        is CommandResult.Failure -> fail("command resulted in failure $this")
    }
}
