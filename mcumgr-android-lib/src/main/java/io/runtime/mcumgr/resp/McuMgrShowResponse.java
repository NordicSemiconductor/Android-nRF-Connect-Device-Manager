/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.resp;

public class McuMgrShowResponse extends McuMgrSimpleResponse {
    public int next_index;
    public LogResult[] logs;

    public static class LogResult {
        public String name;
        public int type;
        public McuMgrLogResponse.LogEntry[] entries;
    }
}
