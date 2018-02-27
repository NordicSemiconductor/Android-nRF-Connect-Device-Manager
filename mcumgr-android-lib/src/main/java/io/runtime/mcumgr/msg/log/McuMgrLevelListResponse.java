/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.msg.log;


import io.runtime.mcumgr.msg.McuMgrResponse;

public class McuMgrLevelListResponse extends McuMgrResponse {
    public String[] level_map;
}
