package com.juul.mcumgr.message

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
    val tasks: Map<String, Task>
) : Response() {

    data class Task(
        val priority: Int,
        val taskId: Int,
        val state: Int,
        val stackUse: Int,
        val stackSize: Int,
        val contextSwitchCount: Int,
        val runtime: Int,
        val lastCheckIn: Int,
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
