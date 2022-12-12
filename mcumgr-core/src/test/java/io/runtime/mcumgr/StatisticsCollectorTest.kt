package io.runtime.mcumgr

import io.runtime.mcumgr.exception.McuMgrErrorException
import io.runtime.mcumgr.managers.StatsManager
import io.runtime.mcumgr.managers.meta.StatCollectionResult
import io.runtime.mcumgr.managers.meta.StatisticsCollector
import io.runtime.mcumgr.mock.MockBleMcuMgrTransport
import io.runtime.mcumgr.mock.MockCoapMcuMgrTransport
import io.runtime.mcumgr.mock.handlers.MockStatsHandler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

private const val GROUP1 = "group1"
private const val GROUP2 = "group2"
private const val GROUP3 = "group3"

class StatisticsCollectorTest {

    private val group1Stats = mapOf(
        GROUP1 to mapOf(
            "stat1" to 1L,
            "stat2" to 2L,
            "stat3" to 3L,
            "stat4" to 4L,
            "stat5" to 5L
        )
    )

    private val group2Stats = mapOf(
        GROUP2 to mapOf(
            "stat1" to 1L,
            "stat2" to 2L,
            "stat3" to 3L,
            "stat4" to 4L,
            "stat5" to 5L
        )
    )

    private val group3Stats = mapOf(
        GROUP3 to mapOf(
            "stat1" to 1L,
            "stat2" to 2L,
            "stat3" to 3L,
            "stat4" to 4L,
            "stat5" to 5L
        )
    )

    private val allStats = group1Stats + group2Stats + group3Stats

    @Test
    fun `collect all stats success`() = runBlocking {
        val resultLock = Channel<StatCollectionResult>(Channel.CONFLATED)
        val statsHandler = MockStatsHandler(McuMgrScheme.COAP_BLE, allStats)
        val statsManager = StatsManager(MockCoapMcuMgrTransport(statsHandler))
        val statsCollector = StatisticsCollector(statsManager)
        statsCollector.collectAll { result ->
            resultLock.trySend(result)
        }
        val result = resultLock.receive()
        require(result is StatCollectionResult.Success) {
            "Expected stat collection result success, was ${result::class.java.canonicalName}"
        }
        assertEquals(allStats, result.statistics)
    }

    @Test
    fun `collect group success`() = runBlocking {
        val resultLock = Channel<StatCollectionResult>(Channel.CONFLATED)
        val statsHandler = MockStatsHandler(McuMgrScheme.COAP_BLE, allStats)
        val statsManager = StatsManager(MockCoapMcuMgrTransport(statsHandler))
        val statsCollector = StatisticsCollector(statsManager)
        statsCollector.collect(GROUP1) { result ->
            resultLock.trySend(result)
        }
        val result = resultLock.receive()
        require(result is StatCollectionResult.Success) {
            "Expected stat collection result success, was ${result::class.java.canonicalName}"
        }
        assertEquals(group1Stats, result.statistics)
    }

    @Test
    fun `collect multiple groups success`() = runBlocking {
        val resultLock = Channel<StatCollectionResult>(Channel.CONFLATED)
        val statsHandler = MockStatsHandler(McuMgrScheme.BLE, allStats)
        val statsManager = StatsManager(MockBleMcuMgrTransport(statsHandler))
        val statsCollector = StatisticsCollector(statsManager)
        statsCollector.collectGroups(listOf(GROUP1, GROUP2)) { result ->
            resultLock.trySend(result)
        }
        val result = resultLock.receive()
        require(result is StatCollectionResult.Success) {
            "Expected stat collection result success, was $result"
        }
        assertEquals(group1Stats + group2Stats, result.statistics)
    }

    @Test
    fun `collect all with filter success`() = runBlocking {
        val resultLock = Channel<StatCollectionResult>(Channel.CONFLATED)
        val statsHandler = MockStatsHandler(McuMgrScheme.BLE, allStats)
        val statsManager = StatsManager(MockBleMcuMgrTransport(statsHandler))
        val statsCollector = StatisticsCollector(statsManager)
        statsCollector.collectAll(setOf(GROUP1)) { result ->
            resultLock.trySend(result)
        }
        val result = resultLock.receive()
        require(result is StatCollectionResult.Success) {
            "Expected stat collection result success, was ${result::class.java.canonicalName}"
        }
        assertEquals(group1Stats, result.statistics)
    }

    @Test
    fun `collect all with bad filter failure`() = runBlocking {
        val resultLock = Channel<StatCollectionResult>(Channel.CONFLATED)
        val statsHandler = MockStatsHandler(McuMgrScheme.COAP_BLE, allStats)
        val statsManager = StatsManager(MockCoapMcuMgrTransport(statsHandler))
        val statsCollector = StatisticsCollector(statsManager)
        statsCollector.collectAll(setOf("asdf")) { result ->
            resultLock.trySend(result)
        }
        val result = resultLock.receive()
        require(result is StatCollectionResult.Failure) {
            "Expected stat collection result success, was ${result::class.java.canonicalName}"
        }
    }

    @Test
    fun `collect all cancel success`() = runBlocking {
        val resultLock = Channel<StatCollectionResult>(Channel.CONFLATED)
        val statsHandler = MockStatsHandler(McuMgrScheme.COAP_BLE, allStats)
        val statsManager = StatsManager(MockCoapMcuMgrTransport(statsHandler))
        val statsCollector = StatisticsCollector(statsManager)
        val cancellable = statsCollector.collectAll { result ->
            resultLock.trySend(result)
        }
        cancellable.cancel()
        val result = resultLock.receive()
        require(result is StatCollectionResult.Cancelled) {
            "Expected stat collection result success, was ${result::class.java.canonicalName}"
        }
    }

    @Test
    fun `collect all fail stat read`() = runBlocking {
        val resultLock = Channel<StatCollectionResult>(Channel.CONFLATED)
        val statsHandler = MockStatsHandler(McuMgrScheme.COAP_BLE, allStats)
        val statsManager = StatsManager(MockCoapMcuMgrTransport(statsHandler))
        val statsCollector = StatisticsCollector(statsManager)
        val cancellable = statsCollector.collect("asdf") { result ->
            resultLock.trySend(result)
        }
        cancellable.cancel()
        val result = resultLock.receive()
        require(result is StatCollectionResult.Failure) {
            "Expected stat collection result success, was ${result::class.java.canonicalName}"
        }
        val throwable = result.throwable
        require(throwable is McuMgrErrorException) {
            "Expected McuMgrErrorException, was ${throwable::class.java.canonicalName}"
        }
        assertEquals(throwable.code, McuMgrErrorCode.IN_VALUE)
    }
}




