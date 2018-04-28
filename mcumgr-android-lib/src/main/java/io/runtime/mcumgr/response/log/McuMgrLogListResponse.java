/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.log;

import io.runtime.mcumgr.response.McuMgrResponse;

public class McuMgrLogListResponse extends McuMgrResponse {
    public String[] log_list;
}
