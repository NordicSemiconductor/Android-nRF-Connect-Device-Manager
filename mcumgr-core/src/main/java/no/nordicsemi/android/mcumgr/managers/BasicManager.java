/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package no.nordicsemi.android.mcumgr.managers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import no.nordicsemi.android.mcumgr.McuManager;
import no.nordicsemi.android.mcumgr.McuMgrCallback;
import no.nordicsemi.android.mcumgr.McuMgrErrorCode;
import no.nordicsemi.android.mcumgr.McuMgrGroupReturnCode;
import no.nordicsemi.android.mcumgr.McuMgrTransport;
import no.nordicsemi.android.mcumgr.exception.McuMgrException;
import no.nordicsemi.android.mcumgr.response.HasReturnCode;
import no.nordicsemi.android.mcumgr.response.McuMgrResponse;
import no.nordicsemi.android.mcumgr.response.zephyr.basic.McuMgrZephyrBasicResponse;

/**
 * Basic command group manager for Zephyr-based devices.
 */
@SuppressWarnings("unused")
public class BasicManager extends McuManager {
    public enum ReturnCode implements McuMgrGroupReturnCode {
        /** No error, this is implied if there is no ret value in the response */
        OK(0),

        /** Unknown error occurred. */
        UNKNOWN(1),

        /** Opening of the flash area has failed. */
        FLASH_OPEN_FAILED(2),

        /** Querying the flash area parameters has failed. */
        FLASH_CONFIG_QUERY_FAIL(3),

        /** Erasing the flash area has failed. */
        FLASH_ERASE_FAILED(4);

        private final int mCode;

        ReturnCode(int code) {
            mCode = code;
        }

        public int value() {
            return mCode;
        }

        public static @Nullable BasicManager.ReturnCode valueOf(@Nullable McuMgrResponse.GroupReturnCode returnCode) {
            if (returnCode == null || returnCode.group != GROUP_BASIC) {
                return null;
            }
            for (BasicManager.ReturnCode code : values()) {
                if (code.value() == returnCode.rc) {
                    return code;
                }
            }
            return UNKNOWN;
        }
    }

    public interface Response extends HasReturnCode {

        @Nullable
        default BasicManager.ReturnCode getOsReturnCode() {
            McuMgrResponse.GroupReturnCode groupReturnCode = getGroupReturnCode();
            if (groupReturnCode == null) {
                if (getReturnCodeValue() == McuMgrErrorCode.OK.value()) {
                    return BasicManager.ReturnCode.OK;
                }
                return BasicManager.ReturnCode.UNKNOWN;
            }
            return BasicManager.ReturnCode.valueOf(groupReturnCode);
        }
    }

    // Command IDs
    // https://github.com/nrfconnect/sdk-zephyr/blob/master/include/mgmt/mcumgr/zephyr_groups.h
    private final static int ID_ERASE_STORAGE = 0;

    /**
     * Construct an stats manager.
     *
     * @param transport the transport to use to send commands.
     */
    public BasicManager(@NotNull McuMgrTransport transport) {
        super(GROUP_BASIC, transport);
    }

    /**
     * Erase application storage partition (asynchronous).
     *
     * @param callback the asynchronous callback.
     */
    public void eraseStorage(@NotNull McuMgrCallback<McuMgrZephyrBasicResponse> callback) {
        send(OP_WRITE, ID_ERASE_STORAGE, null, DEFAULT_TIMEOUT, McuMgrZephyrBasicResponse.class, callback);
    }

    /**
     * Erase application storage partition (synchronous).
     *
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrZephyrBasicResponse eraseStorage() throws McuMgrException {
        return send(OP_WRITE, ID_ERASE_STORAGE, null, DEFAULT_TIMEOUT, McuMgrZephyrBasicResponse.class);
    }
}
