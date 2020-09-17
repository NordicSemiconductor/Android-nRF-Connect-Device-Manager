package com.juul.mcumgr

import com.juul.mcumgr.command.System
import com.juul.mcumgr.util.mapResponse
import com.juul.mcumgr.util.toUnitResult
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.jvm.Throws

private const val MCUMGR_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZZZZZ"

class SystemManager(val transport: Transport) {

    suspend fun echo(echo: String): CommandResult<EchoResponse> {
        val request = System.EchoRequest(echo)
        return transport.send(
            request,
            System.EchoResponse::class
        ).mapResponse { response ->
            EchoResponse(response.echo)
        }
    }

    suspend fun consoleEchoControl(
        enabled: Boolean
    ): CommandResult<Unit> {
        val request = System.ConsoleEchoControlRequest(enabled)
        return transport.send(
            request,
            System.ConsoleEchoControlResponse::class
        ).toUnitResult()
    }

    suspend fun taskStats(): CommandResult<TaskStatsResponse> {
        val request = System.TaskStatsRequest
        return transport.send(
            request,
            System.TaskStatsResponse::class
        ).mapResponse { response ->
            val tasks = response.tasks.mapValues { (_, task) ->
                TaskStatsResponse.Task(
                    task.priority,
                    task.taskId,
                    task.state,
                    task.stackUse,
                    task.stackSize,
                    task.contextSwitchCount,
                    task.runtime,
                    task.lastCheckIn,
                    task.nextCheckIn
                )
            }
            TaskStatsResponse(tasks)
        }
    }

    suspend fun memoryPoolStats(): CommandResult<MemoryPoolStatsResponse> {
        val request = System.MemoryPoolStatsRequest
        return transport.send(
            request,
            System.MemoryPoolStatsResponse::class
        ).mapResponse { response ->
            val memoryPools = response.memoryPools.mapValues { (_, memoryPool) ->
                MemoryPoolStatsResponse.MemoryPool(
                    memoryPool.blockSize,
                    memoryPool.blocks,
                    memoryPool.freeBlocks,
                    memoryPool.freeMinimum
                )
            }
            MemoryPoolStatsResponse(memoryPools)
        }
    }

    suspend fun readDatetime(): CommandResult<ReadDatetimeResponse> {
        val request = System.ReadDatetimeRequest
        return transport.send(
            request,
            System.ReadDatetimeResponse::class
        ).mapResponse { response ->
            ReadDatetimeResponse(response.datetime)
        }
    }

    suspend fun writeDatetime(date: Date, timeZone: TimeZone): CommandResult<Unit> {
        val request = System.WriteDatetimeRequest(dateToString(date, timeZone))
        return transport.send(
            request,
            System.WriteDatetimeResponse::class
        ).toUnitResult()
    }

    suspend fun reset(): CommandResult<Unit> {
        val request = System.ResetRequest
        return transport.send(
            request,
            System.ResetResponse::class
        ).toUnitResult()
    }
}

data class EchoResponse(
    val echo: String
)

data class TaskStatsResponse(
    /** Map of task names to task stats. */
    val tasks: Map<String, Task>
) {

    /**
     * Contains information about a task running on the device.
     */
    data class Task(
        /** Priority of the task. */
        val priority: Int, // TODO create constants for mynewt & zephyr task priorities
        /** ID of the task. */
        val taskId: Int,
        /**
         * The task state, value meaning differs between Mynewt and Zephyr. See
         * [MynewtState] or [ZephyrState] for parsing and details.
         */
        val state: Int,
        /** Stack usage, in bytes. */
        val stackUse: Int,
        /** Size of this task's stack. */
        val stackSize: Int,
        /**
         * Total number of times this task has been context switched during
         * execution.
         */
        val contextSwitchCount: Int,
        /** Total task run time. */
        val runtime: Int,
        /** Last check-in time. */
        val lastCheckIn: Int,
        /** Next check-in time. */
        val nextCheckIn: Int
    ) {

        /**
         * Task state for Mynewt OS.
         *
         * Parse the task state using [valueOf]. Task state is either [Ready]
         * or [Sleep].
         */
        sealed class MynewtState(val value: Int) {

            object Ready : MynewtState(1)
            object Sleep : MynewtState(2)

            companion object {

                fun valueOf(value: Int): MynewtState? {
                    return when (value) {
                        Ready.value -> Ready
                        Sleep.value -> Sleep
                        else -> null
                    }
                }
            }
        }

        /**
         * Task state for Zephyr OS.
         */
        data class ZephyrState(val value: Int) {
            val isDummy: Boolean = value and (1 shl 0) != 0
            val isPending: Boolean = value and (1 shl 1) != 0
            val isPrestart: Boolean = value and (1 shl 2) != 0
            val isDead: Boolean = value and (1 shl 3) != 0
            val isSuspended: Boolean = value and (1 shl 4) != 0
            val isQueued: Boolean = value and (1 shl 5) != 0
        }
    }
}

data class MemoryPoolStatsResponse(
    /** Map of memory pool names to stats. */
    val memoryPools: Map<String, MemoryPool>
) {
    /**
     * Information describing a memory pool.
     */
    data class MemoryPool(
        /** Size of the memory blocks in the pool.  */
        val blockSize: Int,
        /** Number of memory blocks in the pool.  */
        val blocks: Int,
        /** Number of free memory blocks.  */
        val freeBlocks: Int,
        /** Minimum number of free memory blocks ever.  */
        val freeMinimum: Int
    )
}

data class ReadDatetimeResponse(
    val datetime: String
) {
    /** The datetime from the response parsed to a [Date]. */
    val date: Date = stringToDate(datetime)
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
