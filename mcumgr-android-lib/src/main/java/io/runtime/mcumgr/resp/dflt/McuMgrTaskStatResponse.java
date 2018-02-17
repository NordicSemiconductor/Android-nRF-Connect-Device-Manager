/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.resp.dflt;

import java.util.Map;

import io.runtime.mcumgr.resp.McuMgrSimpleResponse;

public class McuMgrTaskStatResponse extends McuMgrSimpleResponse {
    public Map<String, TaskStat> tasks;

    public static class TaskStat {
        public int prio;
        public int tid;
        public int state;
        public int stkuse;
        public int stksiz;
        public int cswcnt;
        public int runtime;
        public int last_checkin;
        public int next_checkin;
    }
}
