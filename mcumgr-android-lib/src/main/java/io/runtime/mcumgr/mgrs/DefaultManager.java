package io.runtime.mcumgr.mgrs;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import io.runtime.mcumgr.McuManager;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrResponse;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.McuMgrException;

/**
 * Default command group manager.
 */
public class DefaultManager extends McuManager {

    // Command IDs
    public final static int ID_ECHO = 0;
    public final static int ID_CONS_ECHO_CTRL = 1;
    public final static int ID_TASKSTATS = 2;
    public final static int ID_MPSTATS = 3;
    public final static int ID_DATETIME_STR = 4;
    public final static int ID_RESET = 5;

    /**
     * Construct an default manager.
     *
     * @param transport the transport to use to send commands.
     */
    public DefaultManager(McuMgrTransport transport) {
        super(GROUP_DEFAULT, transport);
    }

    //******************************************************************
    // Default Commands
    //******************************************************************

    /**
     * Echo a string (asynchronous).
     *
     * @param echo     the string to echo
     * @param callback the asynchronous callback
     */
    public void echo(String echo, McuMgrCallback callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("d", echo);
        send(OP_WRITE, ID_ECHO, payloadMap, callback);
    }

    /**
     * Echo a string (synchronous).
     *
     * @param echo the string to echo
     * @return the response
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrResponse echo(String echo) throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("d", echo);
        return send(OP_WRITE, ID_ECHO, payloadMap);
    }

    /**
     * Set the console echo on the device (synchronous).
     *
     * @param echo     whether or not to echo to the console
     * @param callback the asynchronous callback
     */
    public void consoleEcho(boolean echo, McuMgrCallback callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("echo", echo);
        send(OP_WRITE, ID_CONS_ECHO_CTRL, payloadMap, callback);
    }

    /**
     * Set the console echo on the device (synchronous).
     *
     * @param echo whether or not to echo to the console
     * @return the response
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrResponse consoleEcho(boolean echo) throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("echo", echo);
        return send(OP_WRITE, ID_CONS_ECHO_CTRL, payloadMap);
    }

    /**
     * Get task statistics from the device (asynchronous).
     *
     * @param callback the asynchronous callback
     */
    public void taskstats(McuMgrCallback callback) {
        send(OP_READ, ID_TASKSTATS, null, callback);
    }

    /**
     * Get task statistics from the device (synchronous).
     *
     * @return the response
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrResponse taskstats() throws McuMgrException {
        return send(OP_READ, ID_TASKSTATS, null);
    }

    /**
     * Get memory pool statistics from the device (asynchronous).
     *
     * @param callback the asynchronous callback
     */
    public void mpstat(McuMgrCallback callback) {
        send(OP_READ, ID_MPSTATS, null, callback);
    }

    /**
     * Get memory pool statistics from the device (synchronous).
     *
     * @return the response
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrResponse mpstat() throws McuMgrException {
        return send(OP_READ, ID_MPSTATS, null);
    }

    /**
     * Read the date and time on the device (asynchronous).
     *
     * @param callback the asynchronous callback
     */
    public void readDatetime(McuMgrCallback callback) {
        send(OP_READ, ID_DATETIME_STR, null, callback);
    }

    /**
     * Read the date and time on the device (synchronous).
     *
     * @return the response
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrResponse readDatetime() throws McuMgrException {
        return send(OP_READ, ID_DATETIME_STR, null);
    }

    /**
     * Write the date and time on the device (asynchronous).
     * <p>
     * If date or timeZone are null, the current value will be used.
     *
     * @param date     the date to set the device to
     * @param timeZone the timezone to use with the date
     * @param callback the asynchronous callback
     */
    public void writeDatetime(Date date, TimeZone timeZone, McuMgrCallback callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("datetime", formatDate(date, timeZone));
        send(OP_WRITE, ID_DATETIME_STR, payloadMap, callback);
    }

    /**
     * Write the date and time on the device (synchronous).
     * <p>
     * If date or timeZone are null, the current value will be used.
     *
     * @param date     the date to set the device to
     * @param timeZone the timezone to use with the date
     * @return the response
     */
    public McuMgrResponse writeDatetime(Date date, TimeZone timeZone) throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("datetime", formatDate(date, timeZone));
        return send(OP_WRITE, ID_DATETIME_STR, payloadMap);
    }

    /**
     * Reset the device (synchronous).
     *
     * @param callback the asynchronous callback
     */
    public void reset(McuMgrCallback callback) {
        send(OP_WRITE, ID_RESET, null, callback);
    }

    /**
     * Reset the device (asynchronous).
     *
     * @return the response
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrResponse reset() throws McuMgrException {
        return send(OP_WRITE, ID_RESET, null);
    }

    //******************************************************************
    // Default Command Response POJOs
    //******************************************************************

    /**
     * Response to an echo command.
     */
    public static class EchoResponse extends McuMgrResponse.BaseResponse {
        public String r;
    }

    /**
     * Response to a taskstats command.
     */
    public static class TaskstatsResponse extends McuMgrResponse.BaseResponse {
        public Map<String, Taskstat> tasks;
    }

    public static class Taskstat {
        public int prio;
        public int tid;
        public int state;
        public int stkuse;
        public int stksiz;
        public int cswcnt;
        public int runtime;
        public int last_checkin;
        public int next_checkin;
    }

    /**
     * Response to an mpstat command.
     */
    public static class MpstatResponse extends McuMgrResponse.BaseResponse {
        public Map<String, Mpstat> mpools;
    }

    public static class Mpstat {
        public int blksiz;
        public int nblks;
        public int nfree;
        public int min;
    }

    /**
     * Response to a read datetime command.
     */
    public static class ReadDatetimeResponse extends McuMgrResponse.BaseResponse {
        public String datetime;
    }
}
