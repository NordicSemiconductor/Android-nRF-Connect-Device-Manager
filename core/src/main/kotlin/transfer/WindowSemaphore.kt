package com.juul.mcumgr.transfer

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock

/**
 * A wrapper around a counting semaphore to manage how many upload or download requests can be
 * made simultaneously.
 *
 * The window starts at size 1 and grows by 1 with every successful completion until the capacity
 * is reached or a failed completion is received. If the capacity is reached, the window size is
 * maintained until a failed completion. On failure, the window will shrink by 1 and be flagged such
 * that the window will never grow again.
 */
internal class WindowSemaphore(
    private val capacity: Int
) {

    val currentSize: Int
        get() = size

    private var size = 1
    private var growing = true
    private val mutex = Mutex()
    private val semaphore = Semaphore(capacity, capacity - size)

    /**
     * Acquire a permit, reduing the number of available permits by one. If the number of permits
     * is 0, suspend until one is released.
     */
    suspend fun acquire() {
        semaphore.acquire()
    }

    /**
     * Complete a permit successfully. Release the semaphore and, If the window is still growing
     * and under capacity, increase the size and release another permit.
     */
    suspend fun success() {
        increaseSize()
        semaphore.release()
    }

    /**
     * Fail a permit. Reduce the size and stop growing. Does not release the semaphore.
     *
     * This function should be followed by a call to [recover] in order to release the semaphore if
     * the retries succeed.
     */
    suspend fun fail() = reduceSize()

    /**
     * Recover from a [fail]. Does not effect the size of the window, only releases the semaphore.
     */
    fun recover() = semaphore.release()

    private suspend fun increaseSize(): Unit = mutex.withLock {
        if (size < capacity && growing) {
            size++
            semaphore.release()
        }
    }

    private suspend fun reduceSize(): Unit = mutex.withLock {
        growing = false
        if (size > 1) {
            size--
        }
    }
}
