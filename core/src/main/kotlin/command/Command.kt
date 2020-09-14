package com.juul.mcumgr.command

import com.juul.mcumgr.stringToDate
import java.util.Date

sealed class Request

sealed class Response

//==========================
// System
//==========================

data class EchoRequest(
    val echo: String
) : Request()

data class EchoResponse(
    val echo: String
) : Response()

data class ConsoleEchoControlRequest(
    val enabled: Boolean
) : Request()

object ConsoleEchoControlResponse : Response()

object TaskStatsRequest : Request()

data class TaskStatsResponse(
    /** Map of task names to task stats. */
    val tasks: Map<String, Task>
) : Response() {

    /**
     * Contains information about a task running on the device.
     */
    data class Task(
        /** Priority of the task. */
        val priority: Int, // TODO create constants for mynewt & zephyr task priorities
        /** ID of the task. */
        val taskId: Int,
        /**
         * The task state, value meaning differes between Mynewt and Zephyr. See [MynewtState] or
         * [ZephyrState] for parsing and details.
         */
        val state: Int,
        /** Stack usage, in bytes. */
        val stackUse: Int,
        /** Size of this task's stack. */
        val stackSize: Int,
        /** Total number of times this task has been context switched during execution. */
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
         * Parse the task state using [valueOf]. Task state is either [Ready] or [Sleep].
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

object MemoryPoolStatsRequest : Request()

data class MemoryPoolStatsResponse(
    /** Map of memory pool names to stats. */
    val memoryPools: Map<String, MemoryPool>
) : Response() {
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

object ReadDatetimeRequest : Request()

data class ReadDatetimeResponse(
    val datetime: String
) : Response() {
    /** The datetime from the response parsed to a [Date]. */
    val date: Date = stringToDate(datetime)
}

data class WriteDatetimeRequest(
    val datetime: String
) : Request()

object WriteDatetimeResponse : Response()

object ResetRequest : Request()

object ResetResponse : Response()

//==========================
// Image
//==========================

data class ImageWriteRequest(
    val data: ByteArray,
    val offset: Int,
    val size: Int? = null,
    val hash: ByteArray? = null
) : Request()

data class ImageWriteResponse(
    val offset: Int
) : Response()

data class CoreReadRequest(
    val offset: Int
) : Request()

data class CoreReadResponse(
    val data: ByteArray,
    val offset: Int,
    val length: Int? = null
) : Response()

//==========================
// Stats
//==========================

// TODO

//==========================
// Config
//==========================

// TODO

//==========================
// Logs
//==========================

// TODO

//==========================
// Crash
//==========================

// TODO

//==========================
// Run
//==========================

// TODO

//==========================
// File
//==========================

data class FileWriteRequest(
    val fileName: String,
    val data: ByteArray,
    val offset: Int,
    val length: Int? = null
) : Request()

data class FileWriteResponse(
    val offset: Int
) : Response()

data class FileReadRequest(
    val fileName: String,
    val offset: Int
) : Request()

data class FileReadResponse(
    val data: ByteArray,
    val offset: Int,
    val length: Int? = null
) : Response()
