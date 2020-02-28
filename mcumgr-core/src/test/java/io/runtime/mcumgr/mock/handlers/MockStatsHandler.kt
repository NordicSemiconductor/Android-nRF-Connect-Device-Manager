package io.runtime.mcumgr.mock.handlers

import io.runtime.mcumgr.McuMgrErrorCode
import io.runtime.mcumgr.McuMgrHeader
import io.runtime.mcumgr.mock.McuMgrErrorResponse
import io.runtime.mcumgr.mock.McuMgrHandler
import io.runtime.mcumgr.mock.buildMockResponse
import io.runtime.mcumgr.mock.toResponse
import io.runtime.mcumgr.response.McuMgrResponse
import io.runtime.mcumgr.response.stat.McuMgrStatListResponse
import io.runtime.mcumgr.response.stat.McuMgrStatResponse
import io.runtime.mcumgr.util.CBOR

enum class McuMgrStatsCommand(val value: Int) {
    READ(0),
    LIST (1)
}

class MockStatsHandler(
    private val stats: Map<String, Map<String, Long>> = allStats
): McuMgrHandler {

    /**
     * Default stats to query and return when
     */
    companion object DefaultStats {

        const val GROUP1_NAME = "group1"
        const val GROUP2_NAME = "group2"
        const val GROUP3_NAME = "group3"

        const val stat1Name = "stat1"
        const val stat2Name = "stat2"
        const val stat3Name = "stat3"
        const val stat4Name = "stat4"
        const val stat5Name = "stat5"

        const val stat1Value = 1L
        const val stat2Value = 2L
        const val stat3Value = 3L
        const val stat4Value = 4L
        const val stat5Value = 5L

        val group1Stats = mapOf(
            GROUP1_NAME to mapOf(
                stat1Name to stat1Value,
                stat2Name to stat2Value,
                stat3Name to stat3Value,
                stat4Name to stat4Value,
                stat5Name to stat5Value
            )
        )

        val group2Stats = mapOf(
            GROUP2_NAME to mapOf(
                stat1Name to stat1Value,
                stat2Name to stat2Value,
                stat3Name to stat3Value,
                stat4Name to stat4Value,
                stat5Name to stat5Value
            )
        )

        val group3Stats = mapOf(
            GROUP3_NAME to mapOf(
                stat1Name to stat1Value,
                stat2Name to stat2Value,
                stat3Name to stat3Value,
                stat4Name to stat4Value,
                stat5Name to stat5Value
            )
        )

        private val allStats = group1Stats + group2Stats + group3Stats
    }

    /**
     * Handle a request for the stats group
     */
    override fun <T : McuMgrResponse?> handle(
        header: McuMgrHeader,
        payload: ByteArray,
        responseType: Class<T>
    ): T {
        return when (header.commandId) {
            McuMgrStatsCommand.LIST.value -> handleStatsListRequest(header, responseType)
            McuMgrStatsCommand.READ.value -> handleStatsReadRequest(header, payload, responseType)
            else -> throw IllegalArgumentException("Unimplemented command with ID ${header.commandId}")
        }
    }

    /**
     * Handle a stats list request.
     */
    private fun <T : McuMgrResponse?> handleStatsListRequest(
        header: McuMgrHeader,
        responseType: Class<T>
    ): T {
        val response = McuMgrStatListResponse().apply {
            stat_list = stats.keys.toTypedArray()
        }
        val responsePayload = CBOR.toBytes(response)
        return buildMockResponse(header.toResponse(), responsePayload, responseType)
    }

    /**
     * Handle a stats read request.
     */
    private fun <T : McuMgrResponse?> handleStatsReadRequest(
        header: McuMgrHeader,
        payload: ByteArray,
        responseType: Class<T>
    ): T {
        val requestName = CBOR.getString(payload, "name")
        val fields = stats[requestName]
        val response = if (fields != null) {
            McuMgrStatResponse().apply {
                this.name = requestName
                this.fields = fields
            }
        } else {
            // If the stat group is not found, return an error code.
            McuMgrErrorResponse(McuMgrErrorCode.IN_VALUE)
        }
        val responsePayload = CBOR.toBytes(response)
        return buildMockResponse(header.toResponse(), responsePayload, responseType)
    }
}
