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
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.response.config.McuMgrConfigReadResponse;

/**
 * Config command group manager.
 */
@SuppressWarnings("unused")
public class ConfigManager extends McuManager {
    private final static int ID_CONFIG = 0;

    /**
     * Construct an config manager.
     *
     * @param transport the transport to use to send commands.
     */
    public ConfigManager(@NotNull McuMgrTransport transport) {
        super(GROUP_CONFIG, transport);
    }

    /**
     * Read a config variable (asynchronous).
     *
     * @param name     the name of the config variable.
     * @param callback the asynchronous callback.
     */
    public void read(@Nullable String name,
                     @NotNull McuMgrCallback<McuMgrConfigReadResponse> callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("name", name);
        send(OP_READ, ID_CONFIG, payloadMap, McuMgrConfigReadResponse.class, callback);
    }

    /**
     * Read a config variable (synchronous).
     *
     * @param name the name of the config variable.
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrConfigReadResponse read(@Nullable String name) throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("name", name);
        return send(OP_READ, ID_CONFIG, payloadMap, McuMgrConfigReadResponse.class);
    }

    /**
     * Write a value to a config variable (asynchronous).
     *
     * @param name     the name of the config variable.
     * @param value    the value to write.
     * @param save     whether or not to save the value after it is set. A saved value will persist
     *                 in flash across device resets.
     * @param callback the asynchronous callback.
     */
    public void write(@Nullable String name, @Nullable String value, boolean save,
                      @NotNull McuMgrCallback<McuMgrResponse> callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("name", name);
        payloadMap.put("val", value);
        payloadMap.put("save", save);
        send(OP_WRITE, ID_CONFIG, payloadMap, McuMgrResponse.class, callback);
    }

    /**
     * Write a value to a config variable (synchronous).
     *
     * @param name  the name of the config variable.
     * @param value the value to write.
     * @param save  whether or not to save the value after it is set. A saved value will persist
     *              in flash across device resets.
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrResponse write(@Nullable String name, @Nullable String value, boolean save)
            throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("name", name);
        payloadMap.put("val", value);
        payloadMap.put("save", save);
        return send(OP_WRITE, ID_CONFIG, payloadMap, McuMgrResponse.class);
    }
}
