/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import no.nordicsemi.android.mcumgr.McuMgrErrorCode;
import no.nordicsemi.android.mcumgr.response.HasReturnCode;
import no.nordicsemi.android.mcumgr.response.McuMgrResponse;

/**
 * Used to convey errors caused by an {@link McuMgrErrorCode} within a response.
 */
@SuppressWarnings("unused")
public class McuMgrErrorException extends McuMgrException {
    @NotNull
    private final McuMgrErrorCode mCode;

    @Nullable
    private final HasReturnCode.GroupReturnCode mGroupCode;

    public McuMgrErrorException(@NotNull McuMgrErrorCode code) {
        super("Mcu Mgr Error: " + code);
        mCode = code;
        mGroupCode = null;
    }

    public McuMgrErrorException(@NotNull HasReturnCode.GroupReturnCode code) {
        super("Mcu Mgr Error: " + code);
        mCode = McuMgrErrorCode.OK;
        mGroupCode = code;
    }

    public McuMgrErrorException(@NotNull McuMgrResponse response) {
        super("Mcu Mgr Error: " + (response.getGroupReturnCode() != null ? response.getGroupReturnCode() : response.rc));
        mCode = response.getReturnCode();
        mGroupCode = response.getGroupReturnCode();
    }

    /**
     * Get the code which caused this exception to be thrown.
     *
     * @return The McuManager response code which caused this exception to be thrown.
     */
    @NotNull
    public McuMgrErrorCode getCode() {
        return mCode;
    }

    public HasReturnCode.GroupReturnCode getGroupCode() {
        return mGroupCode;
    }
}
