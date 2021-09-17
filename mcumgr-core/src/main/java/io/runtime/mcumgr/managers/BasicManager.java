/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.runtime.mcumgr.managers;

import org.jetbrains.annotations.NotNull;

import io.runtime.mcumgr.McuManager;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.McuMgrResponse;

/**
 * Basic command group manager for Zephyr-based devices.
 */
@SuppressWarnings("unused")
public class BasicManager extends McuManager {

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
    public void eraseStorage(@NotNull McuMgrCallback<McuMgrResponse> callback) {
        send(OP_WRITE, ID_ERASE_STORAGE, null, McuMgrResponse.class, callback);
    }

    /**
     * Erase application storage partition (synchronous).
     *
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrResponse eraseStorage() throws McuMgrException {
        return send(OP_WRITE, ID_ERASE_STORAGE, null, McuMgrResponse.class);
    }
}
