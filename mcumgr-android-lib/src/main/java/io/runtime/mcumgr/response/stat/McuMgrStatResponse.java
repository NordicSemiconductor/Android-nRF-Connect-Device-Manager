/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.stat;

import java.util.Map;

import io.runtime.mcumgr.response.McuMgrResponse;

public class McuMgrStatResponse extends McuMgrResponse {
    /** Module name. */
    public String name;
    /** A map of key-value pairs for the given module. */
    public Map<String, Integer> fields;
}
