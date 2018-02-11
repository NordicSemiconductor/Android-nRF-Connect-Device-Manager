package io.runtime.mcumgr.mgrs;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.runtime.mcumgr.McuManager;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.McuMgrResponse;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.util.CBOR;

/**
 * Log command group manager.
 */
public class LogManager extends McuManager {

    private final static String TAG = "LogManager";

    // Command IDs
    public final static int ID_READ = 0;
    public final static int ID_CLEAR = 1;
    public final static int ID_APPEND = 2;
    public final static int ID_MODULE_LIST = 3;
    public final static int ID_LEVEL_LIST = 4;
    public final static int ID_LOGS_LIST = 5;

    /**
     * Used to keep track of the state of a log.
     */
    public static class LogState {
        public int nextIndex = 0;
        public ArrayList<LogEntry> entries = new ArrayList<>();

        public void reset() {
            nextIndex = 0;
            entries = new ArrayList<>();
        }
    }

    private HashMap<String, LogState> mLogStates = new HashMap<>();

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
    public void show(String logName, Integer minIndex, Date minTimestamp, McuMgrCallback callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        if (logName != null) {
            payloadMap.put("log_name", logName);
        }
        if (minIndex != null) {
            payloadMap.put("index", minIndex);
            if (minTimestamp != null) {
                payloadMap.put("ts", formatDate(minTimestamp, null));
            }
        }
        send(OP_READ, ID_READ, payloadMap, callback);
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
    public McuMgrResponse show(String logName, Integer minIndex, Date minTimestamp)
            throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        if (logName != null) {
            payloadMap.put("log_name", logName);
        }
        if (minIndex != null) {
            payloadMap.put("index", minIndex);
            if (minTimestamp != null) {
                payloadMap.put("ts", formatDate(minTimestamp, null));
            }
        }
        return send(OP_READ, ID_READ, payloadMap);
    }


    /**
     * Clear the logs on a device (asynchronous).
     *
     * @param callback The response callback
     */
    public void clear(McuMgrCallback callback) {
        send(OP_WRITE, ID_CLEAR, null, callback);
    }

    /**
     * Clear the logs on a device (synchronous).
     *
     * @return The response
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrResponse clear() throws McuMgrException {
        return send(OP_WRITE, ID_CLEAR, null);
    }

    /**
     * List the log modules on a device (asynchronous).
     * <p>
     * Note: This is NOT the log name to use to pass into show.
     *
     * @param callback The response callback
     */
    public void moduleList(McuMgrCallback callback) {
        send(OP_READ, ID_MODULE_LIST, null, callback);
    }

    /**
     * List the log modules on a device (synchronous).
     * <p>
     * Note: This is NOT the log name to use to pass into show.
     *
     * @return The response
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrResponse moduleList() throws McuMgrException {
        return send(OP_READ, ID_MODULE_LIST, null);
    }

    /**
     * List the log levels on a device (asynchronous).
     *
     * @param callback The response callback
     */
    public void levelList(McuMgrCallback callback) {
        send(OP_READ, ID_LEVEL_LIST, null, callback);
    }

    /**
     * List the log levels on a device (synchronous).
     *
     * @return The response
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrResponse levelList() throws McuMgrException {
        return send(OP_READ, ID_LEVEL_LIST, null);
    }

    /**
     * List the log names on a device (asynchronous).
     * <p>
     * Note: this is the "log name" to pass into show to read logs.
     *
     * @param callback The response callback
     */
    public void logsList(McuMgrCallback callback) {
        send(OP_READ, ID_LOGS_LIST, null, callback);
    }

    /**
     * List the log names on a device (synchronous).
     * <p>
     * Note: this is the "log name" to pass into show to read logs.
     *
     * @return The response
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrResponse logsList() throws McuMgrException {
        return send(OP_READ, ID_LOGS_LIST, null);
    }

    public Map<String, LogState> showAll() {
        // Clear any log states
        mLogStates.clear();
        try {
            Log.d(TAG, "Getting available logs...");
            // Get available logs
            McuMgrResponse response = logsList();
            if (response == null || !response.isSuccess()) {
                Log.e(TAG, "Error occurred getting the list of logs.");
                return null;
            }
            LogListResponse logListResponse = CBOR.toObject(response.getPayload(),
                    LogListResponse.class);
            Log.d(TAG, "Available logs: " + CBOR.toString(logListResponse));

            // For each log, get all the available logs
            for (String logName : logListResponse.log_list) {
                Log.d(TAG, "Getting logs for log " + logName);
                // Put a new LogState mapping if necessary
                LogState logState = mLogStates.get(logName);
                if (logState == null) {
                    logState = new LogState();
                    mLogStates.put(logName, logState);
                }
                // Loop until we run out of entries or encounter a problem
                while (true) {
                    // Get the next set of entries for this log
                    ShowResponse showResponse = showNext(logName);
                    // Check for an error
                    if (showResponse == null) {
                        Log.e(TAG, "Show logs resulted in an error");
                        break;
                    }
                    Log.d(TAG, "Logs: " + CBOR.toString(showResponse));
                    // Check for an index mismatch
                    if (showResponse.next_index < logState.nextIndex) {
                        Log.w(TAG, "Next index mismatch state.nextIndex=" + logState.nextIndex +
                                ", response.nextIndex=" + showResponse.next_index);
                        Log.w(TAG, "Resetting log state.");
                        logState.reset();
                        continue;
                    }
                    // If the logs are null or empty, break to the next log
                    if (showResponse.logs == null || showResponse.logs.length == 0) {
                        Log.e(TAG, "No logs returned in the response.");
                        break;
                    }
                    // Get the log result object
                    LogResult log = showResponse.logs[0];
                    logState.nextIndex = showResponse.next_index;
                    //If er dont have any more entries, break out of this log to the next.s
                    if (log.entries == null || log.entries.length == 0) {
                        Log.d(TAG, "No more entries left for this log.");
                        break;
                    }
                    // Add entries to the list and set the next index
                    logState.entries.addAll(Arrays.asList(log.entries));
                }
            }
            return mLogStates;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (McuMgrException e) {
            e.getCause().printStackTrace();
            Log.e(TAG, "Transport error getting available logs: " + e.getCause().toString());
        }
        return null;
    }

    public ShowResponse showNext(String name) {
        LogState logState = mLogStates.get(name);
        Log.d(TAG, "Show logs: name=" + name + ", nextIndex=" + logState.nextIndex);
        try {
            McuMgrResponse response = show(name, logState.nextIndex, null);
            if (response == null || !response.isSuccess()) {
                Log.e(TAG, "Error occurred getting logs");
                return null;
            }
            return CBOR.toObject(response.getPayload(), ShowResponse.class);
        } catch (McuMgrException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    //******************************************************************
    // Log Manager Response POJOs
    //******************************************************************

    /**
     * Response to {@link LogManager#show(String, Integer, Date)}
     */
    public static class ShowResponse extends McuMgrResponse.BaseResponse {
        public int next_index;
        public LogResult[] logs;
    }

    public static class LogResult {
        public String name;
        public int type;
        public LogEntry[] entries;
    }

    public static class LogEntry {
        public String msg;
        public long ts;
        public int level;
        public int index;
        public int module;
    }

    /**
     * Response to {@link LogManager#logsList()}.
     */
    public static class LogListResponse extends McuMgrResponse.BaseResponse {
        public String[] log_list;
    }

    /**
     * Response to {@link LogManager#moduleList()}.
     */
    public static class ModuleListResponse extends McuMgrResponse.BaseResponse {
        public Map<String, Integer> module_map;
    }
}
