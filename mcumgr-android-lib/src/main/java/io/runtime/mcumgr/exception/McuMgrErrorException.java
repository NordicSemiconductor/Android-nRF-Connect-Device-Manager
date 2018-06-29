/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.exception;

import android.support.annotation.NonNull;

import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;
import io.runtime.mcumgr.response.McuMgrResponse;

/**
 * Used to convey errors caused by an {@link McuMgrErrorCode} within a response. This is used in
 * practice by {@link FirmwareUpgradeManager} to send a failure callback with the
 * {@link McuMgrErrorCode} that caused the failure.
 */
public class McuMgrErrorException extends McuMgrException {
    @NonNull
    private McuMgrErrorCode mCode;

    public McuMgrErrorException(@NonNull McuMgrErrorCode code) {
        super("Mcu Mgr Error: " + code);
        mCode = code;
    }

    public McuMgrErrorException(@NonNull McuMgrResponse response) {
        this(McuMgrErrorCode.valueOf(response.rc));
    }

    /**
     * Get the code which caused this exception to be thrown.
     *
     * @return The McuManager response code which caused this exception to be thrown.
     */
    @NonNull
    public McuMgrErrorCode getCode() {
        return mCode;
    }
}
