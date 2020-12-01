package io.runtime.mcumgr.transfer

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
     * Acquire a permit, reducing the number of available permits by one. If the number of permits
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
        mutex.withLock {
            if (size < capacity && growing) {
                size++
                semaphore.release()
            }
        }
        semaphore.release()
    }

    /**
     * Fail a permit to stop growth. Does not release the semaphore.
     *
     * This function must be followed by a call to [recover] if a retry succeeds.
     */
    suspend fun fail(): Unit = mutex.withLock {
        growing = false
    }

    /**
     * Recover from a [fail]. If the window is able to shrink, reduce the size, otherwise release
     * the permit to avoid deadlock.
     */
    suspend fun recover(): Unit = mutex.withLock {
        if (size > 1) {
            size--
        } else {
            semaphore.release()
        }
    }
}
