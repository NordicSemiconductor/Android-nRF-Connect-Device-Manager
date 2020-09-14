package com.juul.mcumgr

import com.juul.mcumgr.command.ConsoleEchoControlRequest
import com.juul.mcumgr.command.ConsoleEchoControlResponse
import com.juul.mcumgr.command.EchoRequest
import com.juul.mcumgr.command.EchoResponse
import com.juul.mcumgr.command.MemoryPoolStatsRequest
import com.juul.mcumgr.command.MemoryPoolStatsResponse
import com.juul.mcumgr.command.ReadDatetimeRequest
import com.juul.mcumgr.command.ReadDatetimeResponse
import com.juul.mcumgr.command.ResetRequest
import com.juul.mcumgr.command.ResetResponse
import com.juul.mcumgr.command.TaskStatsRequest
import com.juul.mcumgr.command.TaskStatsResponse
import com.juul.mcumgr.command.WriteDatetimeRequest
import com.juul.mcumgr.command.WriteDatetimeResponse
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.jvm.Throws

private const val MCUMGR_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZZZZZ"

class SystemManager(val transport: Transport) {

    suspend fun echo(echo: String): SendResult<EchoResponse> =
        transport.send(EchoRequest(echo))

    suspend fun consoleEchoControl(
        enabled: Boolean
    ): SendResult<ConsoleEchoControlResponse> =
        transport.send(ConsoleEchoControlRequest(enabled))

    suspend fun taskStats(): SendResult<TaskStatsResponse> =
        transport.send(TaskStatsRequest)

    suspend fun memoryPoolStats(): SendResult<MemoryPoolStatsResponse> =
        transport.send(MemoryPoolStatsRequest)

    suspend fun readDatetime(): SendResult<ReadDatetimeResponse> =
        transport.send(ReadDatetimeRequest)

    suspend fun writeDatetime(date: Date, timeZone: TimeZone): SendResult<WriteDatetimeResponse> =
        transport.send(WriteDatetimeRequest(dateToString(date, timeZone)))

    suspend fun reset(): SendResult<ResetResponse> =
        transport.send(ResetRequest)
}

/**
 * Format a [Date] and a [TimeZone] into a mcumgr formatted datetime.
 */
internal fun dateToString(date: Date, timeZone: TimeZone): String {
    val mcumgrFormat = SimpleDateFormat(MCUMGR_DATE_FORMAT, Locale("US"))
    mcumgrFormat.timeZone = timeZone
    return mcumgrFormat.format(date)
}

/**
 * Parse an mcumgr datetime string into a [Date]
 */
@Throws(ParseException::class)
internal fun stringToDate(dateString: String): Date {
    val format = SimpleDateFormat(MCUMGR_DATE_FORMAT, Locale("US"))
    return format.parse(dateString)
}
