/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.resp;

import java.util.Map;

public class McuMgrStatResponse extends McuMgrSimpleResponse {
    public String name;
    public Map<String, Integer> fields;
}
