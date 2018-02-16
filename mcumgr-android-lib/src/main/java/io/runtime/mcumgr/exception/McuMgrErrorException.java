/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.exception;

import io.runtime.mcumgr.McuMgrErrorCode;

public class McuMgrErrorException extends McuMgrException {
    private McuMgrErrorCode mCode;

    public McuMgrErrorException(McuMgrErrorCode code) {
        mCode = code;
    }

    @Override
    public String toString() {
        return "McuMgrErrorException: " + mCode.toString() + " (" + mCode.value() + ")";
    }
}
