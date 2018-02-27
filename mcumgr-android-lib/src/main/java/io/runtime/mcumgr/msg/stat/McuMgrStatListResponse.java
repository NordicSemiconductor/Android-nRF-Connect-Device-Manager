/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.msg.stat;

import io.runtime.mcumgr.msg.McuMgrResponse;

public class McuMgrStatListResponse extends McuMgrResponse {
    private String[] stat_list;
}
