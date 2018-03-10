/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.mgrs;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.runtime.mcumgr.McuManager;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.msg.log.McuMgrLevelListResponse;
import io.runtime.mcumgr.msg.log.McuMgrLogListResponse;
import io.runtime.mcumgr.msg.log.McuMgrLogResponse;
import io.runtime.mcumgr.msg.log.McuMgrModuleListResponse;
import io.runtime.mcumgr.msg.McuMgrResponse;

/**
 * Log command group manager.
 */
public class LogManager extends McuManager {

    private final static String TAG = "LogManager";

    // Command IDs
    private final static int ID_READ = 0;
    private final static int ID_CLEAR = 1;
    private final static int ID_APPEND = 2;
    private final static int ID_MODULE_LIST = 3;
    private final static int ID_LEVEL_LIST = 4;
    private final static int ID_LOGS_LIST = 5;

    /**
     * Construct an image manager.
     *
     * @param transport the transport to use to send commands.
     */
    public LogManager(McuMgrTransport transport) {
        super(GROUP_LOGS, transport);
    }

    /**
     * Show logs from a device (asynchronous).
     * <p>
     * Logs will be shown from the log of the name provided, or all if none. Additionally, logs will
     * only be shown from after the minIndex and minTimestamp if provided (Note: the minimum
     * timestamp will only be used if the minimum index is also provided).
     * <p>
     * This method will only provide a portion of the logs, and return the next index to pull logs
     * from. Therefore, in order to pull all the logs from a device, you may have to call this
     * method multiple times.
     *
     * @param logName      The name of the log to read. If null, the device will report from all logs
     * @param minIndex     The minimum index to pull logs from. If null, the device will read from the
     *                     oldest log.
     * @param minTimestamp The minimum timestamp to pull logs from. This parameter is only used if
     *                     it and minIndex are not null.
     * @param callback     The response callback
     */
    public void show(String logName, Integer minIndex, Date minTimestamp, McuMgrCallback<McuMgrLogResponse>
            callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        if (logName != null) {
            payloadMap.put("log_name", logName);
        }
        if (minIndex != null) {
            payloadMap.put("index", minIndex);
            if (minTimestamp != null) {
                payloadMap.put("ts", dateToString(minTimestamp, null));
            }
        }
        send(OP_READ, ID_READ, payloadMap, McuMgrLogResponse.class, callback);
    }

    /**
     * Show logs from a device (synchronous).
     * <p>
     * Logs will be shown from the log of the name provided, or all if none. Additionally, logs will
     * only be shown from after the minIndex and minTimestamp if provided (Note: the minimum
     * timestamp will only be used if the minimum index is also provided).
     * <p>
     * This method will only provide a portion of the logs, and return the next index to pull logs
     * from. Therefore, in order to pull all the logs from a device, you may have to call this
     * method multiple times.
     *
     * @param logName      The name of the log to read. If null, the device will report from all logs
     * @param minIndex     The minimum index to pull logs from. If null, the device will read from the
     *                     oldest log.
     * @param minTimestamp The minimum timestamp to pull logs from. This parameter is only used if
     *                     it and minIndex are not null.
     * @return The response
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrLogResponse show(String logName, Integer minIndex, Date minTimestamp)
            throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        if (logName != null) {
            payloadMap.put("log_name", logName);
        }
        if (minIndex != null) {
            payloadMap.put("index", minIndex);
            if (minTimestamp != null) {
                payloadMap.put("ts", dateToString(minTimestamp, null));
            }
        }
        return send(OP_READ, ID_READ, payloadMap, McuMgrLogResponse.class);
    }


    /**
     * Clear the logs on a device (asynchronous).
     *
     * @param callback The response callback
     */
    public void clear(McuMgrCallback<McuMgrResponse> callback) {
        send(OP_WRITE, ID_CLEAR, null, McuMgrResponse.class, callback);
    }

    /**
     * Clear the logs on a device (synchronous).
     *
     * @return The response
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrResponse clear() throws McuMgrException {
        return send(OP_WRITE, ID_CLEAR, null, McuMgrResponse.class);
    }

    /**
     * List the log modules on a device (asynchronous).
     * <p>
     * Note: This is NOT the log name to use to pass into show.
     *
     * @param callback The response callback
     */
    public void moduleList(McuMgrCallback<McuMgrModuleListResponse> callback) {
        send(OP_READ, ID_MODULE_LIST, null, McuMgrModuleListResponse.class, callback);
    }

    /**
     * List the log modules on a device (synchronous).
     * <p>
     * Note: This is NOT the log name to use to pass into show.
     *
     * @return The response
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrModuleListResponse moduleList() throws McuMgrException {
        return send(OP_READ, ID_MODULE_LIST, null, McuMgrModuleListResponse.class);
    }

    /**
     * List the log levels on a device (asynchronous).
     *
     * @param callback The response callback
     */
    public void levelList(McuMgrCallback<McuMgrLevelListResponse> callback) {
        send(OP_READ, ID_LEVEL_LIST, null, McuMgrLevelListResponse.class, callback);
    }

    /**
     * List the log levels on a device (synchronous).
     *
     * @return The response
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrLevelListResponse levelList() throws McuMgrException {
        return send(OP_READ, ID_LEVEL_LIST, null, McuMgrLevelListResponse.class);
    }

    /**
     * List the log names on a device (asynchronous).
     * <p>
     * Note: this is the "log name" to pass into show to read logs.
     *
     * @param callback The response callback
     */
    public void logsList(McuMgrCallback<McuMgrLogListResponse> callback) {
        send(OP_READ, ID_LOGS_LIST, null, McuMgrLogListResponse.class, callback);
    }

    /**
     * List the log names on a device (synchronous).
     * <p>
     * Note: this is the "log name" to pass into show to read logs.
     *
     * @return The response
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrLogListResponse logsList() throws McuMgrException {
        return send(OP_READ, ID_LOGS_LIST, null, McuMgrLogListResponse.class);
    }

    /**
     * Get all log entries from all logs on the device.
     * @return a mapping of log name to state
     */
    public synchronized Map<String, State> getAll() {
        HashMap<String, State> logStates = new HashMap<>();
        try {
            // Get available logs
            McuMgrLogListResponse logListResponse = logsList();
            if (logListResponse == null || !logListResponse.isSuccess()) {
                Log.e(TAG, "Error occurred getting the list of logs.");
                return null;
            }
            Log.d(TAG, "Available logs: " + logListResponse.toString());

            // For each log, get all the available logs
            for (String logName : logListResponse.log_list) {
                Log.d(TAG, "Getting logs for log " + logName);
                // Put a new State mapping if necessary
                State state = logStates.get(logName);
                if (state == null) {
                    state = new State(logName);
                    logStates.put(logName, state);
                }
                state = getAllFromState(state);
                logStates.put(state.getName(), state);
            }
            return logStates;
        } catch (McuMgrException e) {
            e.getCause().printStackTrace();
            Log.e(TAG, "Transport error getting available logs: " + e.getCause().toString());
        }
        return null;
    }

    /**
     * Get logs from a state. The logs will be collected from the state's next index and added to
     * the list of entries.
     * @param state The log state to collect logs from
     * @return The log state with updated next index and entry list
     */
    public State getAllFromState(State state) {
        // Loop until we run out of entries or encounter a problem
        while (true) {
            // Get the next set of entries for this log
            McuMgrLogResponse showResponse = showNext(state);
            // Check for an error
            if (showResponse == null) {
                Log.e(TAG, "Show logs resulted in an error");
                break;
            }
//            // Check for an index mismatch
//            if (showResponse.next_index < state.getNextIndex())
//                Log.w(TAG, "Next index mismatch state.nextIndex=" + state.getNextIndex() +
//                        ", response.nextIndex=" + showResponse.next_index);
//                Log.w(TAG, "Resetting log state.");
//                state.reset();
//                continue;
//            }
            // Check that the logs collected are not null or empty
            if (showResponse.logs == null || showResponse.logs.length == 0) {
                Log.e(TAG, "No logs returned in the response.");
                break;
            }
            // Get the log result object
            McuMgrLogResponse.LogResult log = showResponse.logs[0];
            // If we don't have any more entries, break out of this log to the next.
            if (log.entries == null || log.entries.length == 0) {
                Log.d(TAG, "No more entries left for this log.");
                break;
            }
            state.setNextIndex(showResponse.next_index);
            // Add entries to the list and set the next index
            state.getEntries().addAll(Arrays.asList(log.entries));
        }
        return state;
    }

    /**
     * Get the next set of logs from a log state and return the response. This method does not
     * update the log state and only collects as many logs as can fit into a single response.
     * @param state The state to get logs from
     * @return the show response
     */
    public McuMgrLogResponse showNext(State state) {
        Log.d(TAG, "Show logs: name=" + state.getName() +
                ", nextIndex=" + state.getNextIndex());
        try {
            McuMgrLogResponse response = show(state.getName(), state.getNextIndex(), null);
            if (response == null || !response.isSuccess()) {
                Log.e(TAG, "Error occurred getting logs");
                return null;
            }
            return response;
        } catch (McuMgrException e) {
            e.printStackTrace();
        }

        return null;
    }

    //******************************************************************
    // State
    //******************************************************************

    /**
     * Used to track of the state of a log and hold the collected entries.
     */
    public static class State {
        private String mName;

        private int mNextIndex = 0;
        public ArrayList<McuMgrLogResponse.Entry> mEntries = new ArrayList<>();

        public State(String name) {
            this(name, 0);
        }

        public State(String name, int nextIndex) {
            mName = name;
            mNextIndex = nextIndex;
        }

        public void reset() {
            mNextIndex = 0;
            mEntries = new ArrayList<>();
        }

        public String getName() {
            return mName;
        }

        public int getNextIndex() {
            return mNextIndex;
        }

        public void setNextIndex(int nextIndex) {
            mNextIndex = nextIndex;
        }

        public List<McuMgrLogResponse.Entry> getEntries() {
            return mEntries;
        }
    }
}
