/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.resp;

public class McuMgrStatListResponse implements McuMgrResponse {
    private String[] stat_list;

    /* TODO */
    @Override
    public boolean isSuccess() {
        return true;
    }
}
