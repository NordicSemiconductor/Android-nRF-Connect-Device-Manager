package io.runtime.mcumgr.managers.meta

import io.runtime.mcumgr.McuMgrCallback
import io.runtime.mcumgr.exception.McuMgrErrorException
import io.runtime.mcumgr.exception.McuMgrException
import io.runtime.mcumgr.managers.StatsManager
import io.runtime.mcumgr.response.stat.McuMgrStatListResponse
import io.runtime.mcumgr.response.stat.McuMgrStatResponse
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Result of statistic collections.
 */
sealed class StatCollectionResult {
    data class Success(val statistics: Map<String, Map<String, Long>>): StatCollectionResult()
    data class Cancelled(val statistics: Map<String, Map<String, Long>>): StatCollectionResult()
    data class Failure(val throwable: Throwable): StatCollectionResult()
}

/**
 * Callback for statistic collections.
 */
typealias StatCollectionCallback = (StatCollectionResult) -> Unit

/**
 * Non-blocking cancellable interface for cancelling an ongoing task.
 */
interface Cancellable {
    fun cancel()
}

/**
 * Collects stats from a device.
 */
class StatisticsCollector(private val statsManager: StatsManager) {

    /**
     * Collect stats from a single group by name.
     */
    fun collect(groupName: String, callback: StatCollectionCallback): Cancellable {
        return StatCollection(statsManager, callback).start(listOf(groupName))
    }

    /**
     * Collect from a list of statistic group names.
     */
    fun collectGroups(groupNames: List<String>, callback: StatCollectionCallback): Cancellable {
        return StatCollection(statsManager, callback).start(groupNames)
    }

    /**
     * List the stat group names from the device and collect each which intersects with the filter.
     */
    fun collectAll(filter: Set<String>? = null, callback: StatCollectionCallback): Cancellable {
        val collection = StatCollection(statsManager, callback)
        statsManager.list(object: McuMgrCallback<McuMgrStatListResponse> {

            override fun onResponse(response: McuMgrStatListResponse) {
                // Check for error response.
                if (!response.isSuccess) {
                    callback(StatCollectionResult.Failure(McuMgrErrorException(response)))
                    return
                }
                // Filter statistic group names.
                val groupNames = filter?.intersect(response.stat_list.toSet())?.toList()
                    ?: response.stat_list.toList()
                // Ensure group names in response.
                if (groupNames.isEmpty()) {
                    callback(StatCollectionResult.Failure(
                        IllegalStateException("Statistic group list is empty.")
                    ))
                    return
                }
                // Start collection
                collection.start(groupNames)
            }

            override fun onError(error: McuMgrException) {
                callback(StatCollectionResult.Failure(error))
            }
        })
        return collection
    }
}

/**
 * Manages a single statistics collection.
 */
private class StatCollection(
    private val statsManager: StatsManager,
    private val callback: StatCollectionCallback
): Cancellable {

    private val cancelled = AtomicBoolean(false)
    private val started = AtomicBoolean(false)
    private val result = mutableMapOf<String, Map<String, Long>>()

    /**
     * Start the stat collection for a given list of statistics groups.
     *
     * Start must only be called once per collection and must be provided at least one group to
     * collect from. Otherwise this method will throw an error.
     *
     * @throws IllegalArgumentException If the stat collection has already been started.
     */
    fun start(groupNames: List<String>): Cancellable {
        check(started.compareAndSet(false, true)) { "Cannot call start() twice." }
        if (groupNames.isEmpty()) {
            callback(StatCollectionResult.Failure(IllegalArgumentException("List of group names is empty.")))
            return this
        }
        if (cancelled.get()) {
            callback(StatCollectionResult.Cancelled(result))
            return this
        }
        collect(0, groupNames, callback)
        return this
    }

    private fun collect(index: Int, groupNames: List<String>, callback: StatCollectionCallback) {
        require(index in groupNames.indices) { "Index $index is out of range of groupList." }
        statsManager.read(groupNames[index], object: McuMgrCallback<McuMgrStatResponse> {

            override fun onResponse(response: McuMgrStatResponse) {
                if (!response.isSuccess) {
                    callback(StatCollectionResult.Failure(McuMgrErrorException(response)))
                    return
                }
                result[response.name] = response.fields
                when {
                    index == groupNames.size - 1 -> callback(StatCollectionResult.Success(result))
                    cancelled.get() -> callback(StatCollectionResult.Cancelled(result))
                    else -> collect(index + 1, groupNames, callback)
                }
            }

            override fun onError(error: McuMgrException) {
                error.printStackTrace()
                callback(StatCollectionResult.Failure(error))
            }
        })
    }

    override fun cancel() {
        cancelled.set(true)
    }
}
