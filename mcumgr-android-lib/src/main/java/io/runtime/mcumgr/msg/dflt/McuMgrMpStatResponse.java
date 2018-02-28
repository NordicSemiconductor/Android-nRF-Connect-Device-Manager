/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.msg.dflt;


import java.util.Map;

import io.runtime.mcumgr.msg.McuMgrResponse;

public class McuMgrMpStatResponse extends McuMgrResponse {
    public Map<String, MpStat> mpools;

    public static class MpStat {
        public int blksiz;
        public int nblks;
        public int nfree;
        public int min;
    }
}
