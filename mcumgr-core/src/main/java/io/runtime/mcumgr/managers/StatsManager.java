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
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.stat.McuMgrStatListResponse;
import io.runtime.mcumgr.response.stat.McuMgrStatResponse;

/**
 * Stats command group manager.
 */
@SuppressWarnings("unused")
public class StatsManager extends McuManager {

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
        send(OP_READ, ID_READ, payloadMap, McuMgrStatResponse.class, callback);
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
        return send(OP_READ, ID_READ, payloadMap, McuMgrStatResponse.class);
    }

    /**
     * List the statistic modules (asynchronous).
     *
     * @param callback the asynchronous callback.
     */
    public void list(@NotNull McuMgrCallback<McuMgrStatListResponse> callback) {
        send(OP_READ, ID_LIST, null, McuMgrStatListResponse.class, callback);
    }

    /**
     * List the statistic modules (synchronous).
     *
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrStatListResponse list() throws McuMgrException {
        return send(OP_READ, ID_LIST, null, McuMgrStatListResponse.class);
    }
}
