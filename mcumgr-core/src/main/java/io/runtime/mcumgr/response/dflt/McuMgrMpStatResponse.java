/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.dflt;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@SuppressWarnings("unused")
public class McuMgrMpStatResponse extends McuMgrOsResponse {
    // For Mynewt see:
    // https://github.com/apache/mynewt-core/blob/master/kernel/os/include/os/os_mempool.h

    /**
     * Memory pool information. The keys of this map are the names of the memory pools.
     */
    @JsonProperty("mpools")
    public Map<String, MpStat> mpools;

    @JsonCreator
    public McuMgrMpStatResponse() {}

    /**
     * Information describing a memory pool, used to return OS information
     * to the management layer.
     */
    public static class MpStat {
        /** Size of the memory blocks in the pool. */
        @JsonProperty("blksiz")
        public int blksiz;
        /** Number of memory blocks in the pool. */
        @JsonProperty("nblks")
        public int nblks;
        /** Number of free memory blocks. */
        @JsonProperty("nfree")
        public int nfree;
        /** Minimum number of free memory blocks ever. */
        @JsonProperty("min")
        public int min;

        @JsonCreator
        public MpStat() {}
    }
}
