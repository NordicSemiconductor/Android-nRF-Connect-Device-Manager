/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.msg.log;


import io.runtime.mcumgr.msg.McuMgrResponse;

public class McuMgrLogResponse extends McuMgrResponse {
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
