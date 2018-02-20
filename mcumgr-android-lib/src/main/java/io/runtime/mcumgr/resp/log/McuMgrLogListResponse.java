/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.resp.log;

import io.runtime.mcumgr.resp.McuMgrResponse;

public class McuMgrLogListResponse extends McuMgrResponse {
    public String[] log_list;
}
