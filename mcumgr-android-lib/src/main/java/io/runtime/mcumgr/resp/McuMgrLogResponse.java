/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.resp;

public class McuMgrLogResponse {
    public String name;
    public int type;
    public LogEntry[] entries;

    public static class LogEntry {
        public String msg;
        public long ts;
        public int level;
        public int index;
        public int module;
    }
}
