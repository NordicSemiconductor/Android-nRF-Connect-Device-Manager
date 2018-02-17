/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.resp.dflt;

import java.util.Map;

import io.runtime.mcumgr.resp.McuMgrSimpleResponse;

public class McuMgrMpStatResponse extends McuMgrSimpleResponse {
    public Map<String, Mpstat> mpools;

    public static class Mpstat {
        public int blksiz;
        public int nblks;
        public int nfree;
        public int min;
    }
}
