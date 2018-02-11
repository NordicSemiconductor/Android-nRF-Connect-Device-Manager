package io.runtime.mcumgr.mgrs;

import java.util.HashMap;

import io.runtime.mcumgr.McuManager;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrResponse;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.McuMgrTransport;

/**
 * Config command group manager.
 */
public class ConfigManager extends McuManager {

    public final static int ID_CONFIG = 0;

    /**
     * Construct an config manager.
     *
     * @param transport the transport to use to send commands.
     */
    public ConfigManager(McuMgrTransport transport) {
        super(GROUP_CONFIG, transport);
    }

    /**
     * Read a config variable (asynchronous).
     *
     * @param name     the name of the config variable
     * @param callback the asynchronous callback
     */
    public void read(String name, McuMgrCallback callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("name", name);
        send(OP_READ, ID_CONFIG, payloadMap, callback);
    }

    /**
     * Read a config variable (synchronous).
     *
     * @param name the name of the config variable
     * @return the response
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrResponse read(String name) throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("name", name);
        return send(OP_READ, ID_CONFIG, payloadMap);
    }

    /**
     * Write a value to a config variable (asynchronous).
     *
     * @param name     the name of the config variable
     * @param value    the value to write
     * @param callback the asynchronous callback
     */
    public void write(String name, String value, McuMgrCallback callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("name", name);
        payloadMap.put("val", value);
        send(OP_WRITE, ID_CONFIG, payloadMap, callback);
    }

    /**
     * Write a value to a config variable (synchronous).
     *
     * @param name  the name of the config variable
     * @param value the value to write
     * @return the response
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrResponse write(String name, String value) throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("name", name);
        payloadMap.put("val", value);
        return send(OP_WRITE, ID_CONFIG, payloadMap);
    }

    //******************************************************************
    // Config Manager Response POJOs
    //******************************************************************

    /**
     * POJO representation of {@link ConfigManager#read(String)} response.
     */
    public static class ReadResponse extends McuMgrResponse.BaseResponse {
        public String val;
    }
}
