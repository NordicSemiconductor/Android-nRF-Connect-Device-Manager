/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.log;

import io.runtime.mcumgr.response.McuMgrResponse;

public class McuMgrLogResponse extends McuMgrResponse {
    public int next_index;
    public LogResult[] logs;

    public static class LogResult {
        public String name;
        public int type;
        public Entry[] entries;
    }

    public static class Entry {
        public String msg;
        public long ts;
        public int level;
        public int index;
        public int module;
    }
}
