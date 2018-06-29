/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.stat;

import io.runtime.mcumgr.response.McuMgrResponse;

public class McuMgrStatListResponse extends McuMgrResponse {
    /** A list of modules. */
    public String[] stat_list;
}
