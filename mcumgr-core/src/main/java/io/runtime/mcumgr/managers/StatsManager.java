/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.runtime.mcumgr.managers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

import io.runtime.mcumgr.McuManager;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.McuMgrGroupReturnCode;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.HasReturnCode;
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.response.stat.McuMgrStatListResponse;
import io.runtime.mcumgr.response.stat.McuMgrStatResponse;

/**
 * Stats command group manager.
 */
@SuppressWarnings("unused")
public class StatsManager extends McuManager {
    public enum ReturnCode implements McuMgrGroupReturnCode {
        /** No error, this is implied if there is no ret value in the response */
        OK(0),

        /** Unknown error occurred. */
        UNKNOWN(1),

        /** The provided statistic group name was not found. */
        INVALID_GROUP(2),

        /** The provided statistic name was not found. */
        INVALID_STAT_NAME(3),

        /** The size of the statistic cannot be handled. */
        INVALID_STAT_SIZE(4),

        /** Walk through of statistics was aborted. */
        WALK_ABORTED(5);

        private final int mCode;

        ReturnCode(int code) {
            mCode = code;
        }

        public int value() {
            return mCode;
        }

        public static @Nullable StatsManager.ReturnCode valueOf(@Nullable McuMgrResponse.GroupReturnCode returnCode) {
            if (returnCode == null || returnCode.group != GROUP_STATS) {
                return null;
            }
            for (StatsManager.ReturnCode code : values()) {
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
    private final static int ID_READ = 0;
    private final static int ID_LIST = 1;

    /**
     * Construct an stats manager.
     *
     * @param transport the transport to use to send commands.
     */
    public StatsManager(@NotNull McuMgrTransport transport) {
        super(GROUP_STATS, transport);
    }

    /**
     * Read a statistic module (asynchronous).
     *
     * @param module   the name of the module to read.
     * @param callback the asynchronous callback.
     */
    public void read(@Nullable String module, @NotNull McuMgrCallback<McuMgrStatResponse> callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("name", module);
        send(OP_READ, ID_READ, payloadMap, MEDIUM_TIMEOUT, McuMgrStatResponse.class, callback);
    }

    /**
     * Read a statistic module (synchronous).
     *
     * @param module the name of the module to read.
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrStatResponse read(@Nullable String module) throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("name", module);
        return send(OP_READ, ID_READ, payloadMap, MEDIUM_TIMEOUT, McuMgrStatResponse.class);
    }

    /**
     * List the statistic modules (asynchronous).
     *
     * @param callback the asynchronous callback.
     */
    public void list(@NotNull McuMgrCallback<McuMgrStatListResponse> callback) {
        send(OP_READ, ID_LIST, null, MEDIUM_TIMEOUT, McuMgrStatListResponse.class, callback);
    }

    /**
     * List the statistic modules (synchronous).
     *
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrStatListResponse list() throws McuMgrException {
        return send(OP_READ, ID_LIST, null, MEDIUM_TIMEOUT, McuMgrStatListResponse.class);
    }
}
