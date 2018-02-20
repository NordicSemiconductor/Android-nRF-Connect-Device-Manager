/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.resp.log;

import java.util.Map;

import io.runtime.mcumgr.resp.McuMgrResponse;

public class McuMgrModuleListResponse extends McuMgrResponse {
    public Map<String, Integer> module_map;
}
