/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.managers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import io.runtime.mcumgr.McuManager;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.response.dflt.McuMgrEchoResponse;
import io.runtime.mcumgr.response.dflt.McuMgrMpStatResponse;
import io.runtime.mcumgr.response.dflt.McuMgrReadDateTimeResponse;
import io.runtime.mcumgr.response.dflt.McuMgrTaskStatResponse;

/**
 * Default command group manager.
 */
@SuppressWarnings("unused")
public class DefaultManager extends McuManager {

    // Command IDs
    private final static int ID_ECHO = 0;
    private final static int ID_CONS_ECHO_CTRL = 1;
    private final static int ID_TASKSTATS = 2;
    private final static int ID_MPSTATS = 3;
    private final static int ID_DATETIME_STR = 4;
    private final static int ID_RESET = 5;

    /**
     * Construct an default manager.
     *
     * @param transport the transport to use to send commands.
     */
    public DefaultManager(@NotNull McuMgrTransport transport) {
        super(GROUP_DEFAULT, transport);
    }

    //******************************************************************
    // Default Commands
    //******************************************************************

    /**
     * Echo a string (asynchronous).
     *
     * @param echo     the string to echo.
     * @param callback the asynchronous callback.
     */
    public void echo(@Nullable String echo, @NotNull McuMgrCallback<McuMgrEchoResponse> callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("d", echo);
        send(OP_WRITE, ID_ECHO, payloadMap, McuMgrEchoResponse.class, callback);
    }

    /**
     * Echo a string (synchronous).
     *
     * @param echo the string to echo.
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrEchoResponse echo(@Nullable String echo) throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("d", echo);
        return send(OP_WRITE, ID_ECHO, payloadMap, McuMgrEchoResponse.class);
    }

    /**
     * Set the console echo on the device (synchronous).
     *
     * @param echo     whether or not to echo to the console.
     * @param callback the asynchronous callback.
     */
    public void consoleEcho(boolean echo, @NotNull McuMgrCallback<McuMgrResponse> callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("echo", echo);
        send(OP_WRITE, ID_CONS_ECHO_CTRL, payloadMap, McuMgrResponse.class, callback);
    }

    /**
     * Set the console echo on the device (synchronous).
     *
     * @param echo whether or not to echo to the console.
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrResponse consoleEcho(boolean echo) throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("echo", echo);
        return send(OP_WRITE, ID_CONS_ECHO_CTRL, payloadMap, McuMgrResponse.class);
    }

    /**
     * Get task statistics from the device (asynchronous).
     *
     * @param callback the asynchronous callback.
     */
    public void taskstats(@NotNull McuMgrCallback<McuMgrTaskStatResponse> callback) {
        send(OP_READ, ID_TASKSTATS, null, McuMgrTaskStatResponse.class, callback);
    }

    /**
     * Get task statistics from the device (synchronous).
     *
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrTaskStatResponse taskstats() throws McuMgrException {
        return send(OP_READ, ID_TASKSTATS, null, McuMgrTaskStatResponse.class);
    }

    /**
     * Get memory pool statistics from the device (asynchronous).
     *
     * @param callback the asynchronous callback.
     */
    public void mpstat(@NotNull McuMgrCallback<McuMgrMpStatResponse> callback) {
        send(OP_READ, ID_MPSTATS, null, McuMgrMpStatResponse.class, callback);
    }

    /**
     * Get memory pool statistics from the device (synchronous).
     *
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrMpStatResponse mpstat() throws McuMgrException {
        return send(OP_READ, ID_MPSTATS, null, McuMgrMpStatResponse.class);
    }

    /**
     * Read the date and time on the device (asynchronous).
     *
     * @param callback the asynchronous callback.
     */
    public void readDatetime(@NotNull McuMgrCallback<McuMgrReadDateTimeResponse> callback) {
        send(OP_READ, ID_DATETIME_STR, null, McuMgrReadDateTimeResponse.class, callback);
    }

    /**
     * Read the date and time on the device (synchronous).
     *
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrReadDateTimeResponse readDatetime() throws McuMgrException {
        return send(OP_READ, ID_DATETIME_STR, null, McuMgrReadDateTimeResponse.class);
    }

    /**
     * Write the date and time on the device (asynchronous).
     * <p>
     * If date or timeZone are null, the current value will be used.
     *
     * @param date     the date to set the device to.
     * @param timeZone the timezone to use with the date.
     * @param callback the asynchronous callback.
     */
    public void writeDatetime(@Nullable Date date, @Nullable TimeZone timeZone,
                              @NotNull McuMgrCallback<McuMgrResponse> callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("datetime", dateToString(date, timeZone));
        send(OP_WRITE, ID_DATETIME_STR, payloadMap, McuMgrResponse.class, callback);
    }

    /**
     * Write the date and time on the device (synchronous).
     * <p>
     * If date or timeZone are null, the current value will be used.
     *
     * @param date     the date to set the device to.
     * @param timeZone the timezone to use with the date.
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrResponse writeDatetime(@Nullable Date date, @Nullable TimeZone timeZone)
            throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("datetime", dateToString(date, timeZone));
        return send(OP_WRITE, ID_DATETIME_STR, payloadMap, McuMgrResponse.class);
    }

    /**
     * Reset the device (asynchronous).
     *
     * @param callback the asynchronous callback.
     */
    public void reset(@NotNull McuMgrCallback<McuMgrResponse> callback) {
        send(OP_WRITE, ID_RESET, null, McuMgrResponse.class, callback);
    }

    /**
     * Reset the device (synchronous).
     *
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrResponse reset() throws McuMgrException {
        return send(OP_WRITE, ID_RESET, null, McuMgrResponse.class);
    }
}
