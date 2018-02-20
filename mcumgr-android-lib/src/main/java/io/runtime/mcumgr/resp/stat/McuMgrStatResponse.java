/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.resp.stat;

import java.util.Map;

import io.runtime.mcumgr.resp.McuMgrResponse;

public class McuMgrStatResponse extends McuMgrResponse {
    public String name;
    public Map<String, Integer> fields;
}
