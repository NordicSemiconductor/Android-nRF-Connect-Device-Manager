/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.resp;

import java.util.Map;

public class McuMgrModuleListResponse extends McuMgrSimpleResponse {
    public Map<String, Integer> module_map;
}
