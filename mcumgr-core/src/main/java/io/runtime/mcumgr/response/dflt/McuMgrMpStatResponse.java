/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.dflt;


import java.util.Map;

import io.runtime.mcumgr.response.McuMgrResponse;

@SuppressWarnings("unused")
public class McuMgrMpStatResponse extends McuMgrResponse {
    // For Mynewt see:
    // https://github.com/apache/mynewt-core/blob/master/kernel/os/include/os/os_mempool.h

    /**
     * Memory pool information. The keys of this map are the names of the memory pools.
     */
    public Map<String, MpStat> mpools;

    /**
     * Information describing a memory pool, used to return OS information
     * to the management layer.
     */
    public static class MpStat {
        /** Size of the memory blocks in the pool. */
        public int blksiz;
        /** Number of memory blocks in the pool. */
        public int nblks;
        /** Number of free memory blocks. */
        public int nfree;
        /** Minimum number of free memory blocks ever. */
        public int min;
    }
}
