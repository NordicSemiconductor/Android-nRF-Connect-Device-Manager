package io.runtime.mcumgr.mgrs;

import java.util.HashMap;

import io.runtime.mcumgr.McuManager;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.resp.McuMgrStatListResponse;
import io.runtime.mcumgr.resp.McuMgrStatResponse;

import static io.runtime.mcumgr.McuMgrConstants.GROUP_STATS;
import static io.runtime.mcumgr.McuMgrConstants.OP_READ;

/**
 * Stats command group manager.
 */
public class StatsManager extends McuManager {

	// Command IDs
	private final static int ID_READ = 0;
	private final static int ID_LIST = 1;

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
	public void read(String module, McuMgrCallback<McuMgrStatResponse> callback) {
		HashMap<String, Object> payloadMap = new HashMap<>();
		payloadMap.put("name", module);
		send(OP_READ, ID_READ, payloadMap, McuMgrStatResponse.class, callback);
	}

	/**
	 * Read a statistic module (synchronous).
	 *
	 * @param module the name of the module to read.
	 * @return the response
	 * @throws McuMgrException Transport error. See cause.
	 */
	public McuMgrStatResponse read(String module) throws McuMgrException {
		HashMap<String, Object> payloadMap = new HashMap<>();
		payloadMap.put("name", module);
		return send(OP_READ, ID_READ, payloadMap, McuMgrStatResponse.class);
	}

	/**
	 * List the statistic modules (asynchronous).
	 *
	 * @param callback the asynchronous callback
	 */
	public void list(McuMgrCallback<McuMgrStatListResponse> callback) {
		send(OP_READ, ID_LIST, null, McuMgrStatListResponse.class, callback);
	}

	/**
	 * List the statistic modules (synchronous).
	 *
	 * @return the response
	 * @throws McuMgrException Transport error. See cause.
	 */
	public McuMgrStatListResponse list() throws McuMgrException {
		return send(OP_READ, ID_LIST, null, McuMgrStatListResponse.class);
	}
}
