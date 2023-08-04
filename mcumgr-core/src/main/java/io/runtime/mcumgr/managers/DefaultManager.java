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
import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.McuMgrGroupReturnCode;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.HasReturnCode;
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.response.dflt.McuMgrAppInfoResponse;
import io.runtime.mcumgr.response.dflt.McuMgrEchoResponse;
import io.runtime.mcumgr.response.dflt.McuMgrMpStatResponse;
import io.runtime.mcumgr.response.dflt.McuMgrOsResponse;
import io.runtime.mcumgr.response.dflt.McuMgrParamsResponse;
import io.runtime.mcumgr.response.dflt.McuMgrReadDateTimeResponse;
import io.runtime.mcumgr.response.dflt.McuMgrTaskStatResponse;

/**
 * Default command group manager.
 */
@SuppressWarnings("unused")
public class DefaultManager extends McuManager {
    public enum ReturnCode implements McuMgrGroupReturnCode {
        /** No error, this is implied if there is no ret value in the response */
       OK(0),

        /** Unknown error occurred. */
       UNKNOWN(1),

        /** The provided format value is not valid. */
       INVALID_FORMAT(2);

        private final int mCode;

        ReturnCode(int code) {
            mCode = code;
        }

        public int value() {
            return mCode;
        }

        public static @Nullable DefaultManager.ReturnCode valueOf(@Nullable McuMgrResponse.GroupReturnCode returnCode) {
            if (returnCode == null || returnCode.group != GROUP_DEFAULT) {
                return null;
            }
            for (DefaultManager.ReturnCode code : values()) {
                if (code.value() == returnCode.rc) {
                    return code;
                }
            }
            return UNKNOWN;
        }
    }

    public interface Response extends HasReturnCode {

        @Nullable
        default DefaultManager.ReturnCode getOsReturnCode() {
            McuMgrResponse.GroupReturnCode groupReturnCode = getGroupReturnCode();
            if (groupReturnCode == null) {
                if (getReturnCodeValue() == McuMgrErrorCode.OK.value()) {
                    return DefaultManager.ReturnCode.OK;
                }
                return DefaultManager.ReturnCode.UNKNOWN;
            }
            return DefaultManager.ReturnCode.valueOf(groupReturnCode);
        }
    }

    // Command IDs
    private final static int ID_ECHO = 0;
    private final static int ID_CONS_ECHO_CTRL = 1;
    private final static int ID_TASKSTATS = 2;
    private final static int ID_MPSTATS = 3;
    private final static int ID_DATETIME_STR = 4;
    private final static int ID_RESET = 5;
    private final static int ID_MCUMGR_PARAMS = 6;
    private final static int ID_APP_INFO = 7;

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
        send(OP_WRITE, ID_ECHO, payloadMap, SHORT_TIMEOUT, McuMgrEchoResponse.class, callback);
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
        return send(OP_WRITE, ID_ECHO, payloadMap, SHORT_TIMEOUT, McuMgrEchoResponse.class);
    }

    /**
     * Set the console echo on the device (synchronous).
     *
     * @param echo     whether or not to echo to the console.
     * @param callback the asynchronous callback.
     */
    public void consoleEcho(boolean echo, @NotNull McuMgrCallback<McuMgrOsResponse> callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("echo", echo);
        send(OP_WRITE, ID_CONS_ECHO_CTRL, payloadMap, SHORT_TIMEOUT, McuMgrOsResponse.class, callback);
    }

    /**
     * Set the console echo on the device (synchronous).
     *
     * @param echo whether or not to echo to the console.
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrOsResponse consoleEcho(boolean echo) throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("echo", echo);
        return send(OP_WRITE, ID_CONS_ECHO_CTRL, payloadMap, SHORT_TIMEOUT, McuMgrOsResponse.class);
    }

    /**
     * Get task statistics from the device (asynchronous).
     *
     * @param callback the asynchronous callback.
     */
    public void taskstats(@NotNull McuMgrCallback<McuMgrTaskStatResponse> callback) {
        send(OP_READ, ID_TASKSTATS, null, SHORT_TIMEOUT, McuMgrTaskStatResponse.class, callback);
    }

    /**
     * Get task statistics from the device (synchronous).
     *
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrTaskStatResponse taskstats() throws McuMgrException {
        return send(OP_READ, ID_TASKSTATS, null, SHORT_TIMEOUT, McuMgrTaskStatResponse.class);
    }

    /**
     * Get memory pool statistics from the device (asynchronous).
     *
     * @param callback the asynchronous callback.
     */
    public void mpstat(@NotNull McuMgrCallback<McuMgrMpStatResponse> callback) {
        send(OP_READ, ID_MPSTATS, null, SHORT_TIMEOUT, McuMgrMpStatResponse.class, callback);
    }

    /**
     * Get memory pool statistics from the device (synchronous).
     *
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrMpStatResponse mpstat() throws McuMgrException {
        return send(OP_READ, ID_MPSTATS, null, SHORT_TIMEOUT, McuMgrMpStatResponse.class);
    }

    /**
     * Read the date and time on the device (asynchronous).
     *
     * @param callback the asynchronous callback.
     */
    public void readDatetime(@NotNull McuMgrCallback<McuMgrReadDateTimeResponse> callback) {
        send(OP_READ, ID_DATETIME_STR, null, SHORT_TIMEOUT, McuMgrReadDateTimeResponse.class, callback);
    }

    /**
     * Read the date and time on the device (synchronous).
     *
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrReadDateTimeResponse readDatetime() throws McuMgrException {
        return send(OP_READ, ID_DATETIME_STR, null, SHORT_TIMEOUT, McuMgrReadDateTimeResponse.class);
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
                              @NotNull McuMgrCallback<McuMgrOsResponse> callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("datetime", dateToString(date, timeZone));
        send(OP_WRITE, ID_DATETIME_STR, payloadMap, SHORT_TIMEOUT, McuMgrOsResponse.class, callback);
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
    public McuMgrOsResponse writeDatetime(@Nullable Date date, @Nullable TimeZone timeZone)
            throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("datetime", dateToString(date, timeZone));
        return send(OP_WRITE, ID_DATETIME_STR, payloadMap, SHORT_TIMEOUT, McuMgrOsResponse.class);
    }

    /**
     * Reset the device (asynchronous).
     *
     * @param callback the asynchronous callback.
     */
    public void reset(@NotNull McuMgrCallback<McuMgrOsResponse> callback) {
        send(OP_WRITE, ID_RESET, null, SHORT_TIMEOUT, McuMgrOsResponse.class, callback);
    }

    /**
     * Reset the device (synchronous).
     *
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrOsResponse reset() throws McuMgrException {
        return send(OP_WRITE, ID_RESET, null, SHORT_TIMEOUT, McuMgrOsResponse.class);
    }

    /**
     * Reads McuMgr parameters (asynchronous).
     *
     * @param callback the asynchronous callback.
     */
    public void params(@NotNull McuMgrCallback<McuMgrParamsResponse> callback) {
        send(OP_READ, ID_MCUMGR_PARAMS, null, SHORT_TIMEOUT, McuMgrParamsResponse.class, callback);
    }

    /**
     * Reads McuMgr parameters (synchronous).
     *
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrResponse params() throws McuMgrException {
        return send(OP_READ, ID_MCUMGR_PARAMS, null, SHORT_TIMEOUT, McuMgrParamsResponse.class);
    }

    /**
     * Reads OS/Application Info (asynchronous).
     *
     * @param format Format specifier of returned response, fields are appended in their natural
     *               ascending index order, not the order of characters that are received by the command.
     *               Format specifiers:
     *               <ul>
     *                   <li><b>s</b> Kernel name</li>
     *                   <li><b>n</b> Node name</li>
     *                   <li><b>r</b> Kernel release</li>
     *                   <li><b>v</b> Kernel version</li>
     *                   <li><b>b</b> Build date and time</li>
     *                   <li><b>m</b> Machine</li>
     *                   <li><b>p</b> Processor</li>
     *                   <li><b>i</b> Hardware platform</li>
     *                   <li><b>o</b> Operating system</li>
     *                   <li><b>a</b> All fields (shorthand for all above options)</li>
     *               </ul>
     *               If this option is not provided, the <b>s</b> Kernel name option will be used.
     * @param callback the asynchronous callback.
     */
    public void appInfo(@Nullable String format, @NotNull McuMgrCallback<McuMgrAppInfoResponse> callback) {
        HashMap<String, Object> payloadMap = null;
        if (format != null) {
            payloadMap = new HashMap<>();
            payloadMap.put("format", format);
        }
        send(OP_READ, ID_APP_INFO, payloadMap, SHORT_TIMEOUT, McuMgrAppInfoResponse.class, callback);
    }

    /**
     * Reads OS/Application Info (synchronous).
     *
     * @param format Format specifier of returned response, fields are appended in their natural
     *               ascending index order, not the order of characters that are received by the command.
     *               Format specifiers:
     *               <ul>
     *                   <li><b>s</b> Kernel name</li>
     *                   <li><b>n</b> Node name</li>
     *                   <li><b>r</b> Kernel release</li>
     *                   <li><b>v</b> Kernel version</li>
     *                   <li><b>b</b> Build date and time</li>
     *                   <li><b>m</b> Machine</li>
     *                   <li><b>p</b> Processor</li>
     *                   <li><b>i</b> Hardware platform</li>
     *                   <li><b>o</b> Operating system</li>
     *                   <li><b>a</b> All fields (shorthand for all above options)</li>
     *               </ul>
     *               If this option is not provided, the <b>s</b> Kernel name option will be used.
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrResponse params(@Nullable String format) throws McuMgrException {
        HashMap<String, Object> payloadMap = null;
        if (format != null) {
            payloadMap = new HashMap<>();
            payloadMap.put("format", format);
        }
        return send(OP_READ, ID_APP_INFO, payloadMap, SHORT_TIMEOUT, McuMgrParamsResponse.class);
    }
}
