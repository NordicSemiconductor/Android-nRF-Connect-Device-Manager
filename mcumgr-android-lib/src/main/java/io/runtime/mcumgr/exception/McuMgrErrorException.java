/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.exception;

import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;

/**
 * Used to convey errors caused by an {@link McuMgrErrorCode} within a response. This is used in
 * practice by {@link FirmwareUpgradeManager} to send a failure callback with the
 * {@link McuMgrErrorCode} that caused the failure.
 */
public class McuMgrErrorException extends McuMgrException {
    private McuMgrErrorCode mCode;

    public McuMgrErrorException(McuMgrErrorCode code) {
        mCode = code;
    }

    /**
     * Get the code which caused this exception to be thrown.
     * @return the McuManager response code which caused this exception to be thrown
     */
    public McuMgrErrorCode getCode() {
        return mCode;
    }

    @Override
    public String toString() {
        return "McuMgrErrorException: " + mCode.toString() + " (" + mCode.value() + ")";
    }
}
