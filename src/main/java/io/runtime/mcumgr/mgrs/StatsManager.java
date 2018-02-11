package io.runtime.mcumgr.mgrs;

import java.util.HashMap;

import io.runtime.mcumgr.McuManager;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrResponse;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.McuMgrTransport;

/**
 * Stats command group manager.
 */
public class StatsManager extends McuManager {

    // Command IDs
    public final static int ID_READ = 0;
    public final static int ID_LIST = 1;

    /**
     * Construct an stats manager.
     *
     * @param transport the transport to use to send commands.
     */
    public StatsManager(McuMgrTransport transport) {
        super(GROUP_STATS, transport);
    }

    /**
     * Read a statistic module (asynchronous).
     *
     * @param module   the name of the module to read
     * @param callback the asynchronous callback
     */
    public void read(String module, McuMgrCallback callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("name", module);
        send(OP_READ, ID_READ, payloadMap, callback);
    }

    /**
     * Read a statistic module (synchronous).
     *
     * @param module the name of the module to read.
     * @return the response
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrResponse read(String module) throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("name", module);
        return send(OP_READ, ID_READ, payloadMap);
    }

    /**
     * List the statistic modules (asynchronous).
     *
     * @param callback the asynchronous callback
     */
    public void list(McuMgrCallback callback) {
        send(OP_READ, ID_LIST, null, callback);
    }

    /**
     * List the statistic modules (synchronous).
     *
     * @return the response
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrResponse list() throws McuMgrException {
        return send(OP_READ, ID_LIST, null);
    }
}
