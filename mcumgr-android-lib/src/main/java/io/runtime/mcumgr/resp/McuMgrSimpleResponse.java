/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.resp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.runtime.mcumgr.McuMgrErrorCode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class McuMgrSimpleResponse implements McuMgrResponse {
    public int rc;

    @Override
    public boolean isSuccess() {
        return true;
    }

    public McuMgrErrorCode getRcCode() {
        return McuMgrErrorCode.valueOf(rc);
    }
}
