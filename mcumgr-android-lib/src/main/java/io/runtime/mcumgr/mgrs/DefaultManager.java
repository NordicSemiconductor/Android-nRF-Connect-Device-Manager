/*
 *  Copyright (c) Intellinium SAS, 2014-present
 *  All Rights Reserved.
 *
 *  NOTICE:  All information contained herein is, and remains
 *  the property of Intellinium SAS and its suppliers,
 *  if any.  The intellectual and technical concepts contained
 *  herein are proprietary to Intellinium SAS
 *  and its suppliers and may be covered by French and Foreign Patents,
 *  patents in process, and are protected by trade secret or copyright law.
 *  Dissemination of this information or reproduction of this material
 *  is strictly forbidden unless prior written permission is obtained
 *  from Intellinium SAS.
 */
/* TODO: add runtime copyright */

package io.runtime.mcumgr.mgrs;

import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import io.runtime.mcumgr.McuManager;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.resp.McuMgrEchoResponse;
import io.runtime.mcumgr.resp.McuMgrMpStatResponse;
import io.runtime.mcumgr.resp.McuMgrReadDateTimeResponse;
import io.runtime.mcumgr.resp.McuMgrSimpleResponse;
import io.runtime.mcumgr.resp.McuMgrTaskStatResponse;

import static io.runtime.mcumgr.McuMgrConstants.GROUP_DEFAULT;
import static io.runtime.mcumgr.McuMgrConstants.OP_READ;
import static io.runtime.mcumgr.McuMgrConstants.OP_WRITE;

/**
 * Default command group manager.
 */
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
	public void echo(String echo, McuMgrCallback<McuMgrEchoResponse> callback) {
		HashMap<String, Object> payloadMap = new HashMap<>();
		payloadMap.put("d", echo);
		send(OP_WRITE, ID_ECHO, payloadMap, McuMgrEchoResponse.class, callback);
	}

	/**
	 * Echo a string (synchronous).
	 *
	 * @param echo the string to echo
	 * @return the response
	 * @throws McuMgrException Transport error. See cause.
	 */
	public McuMgrEchoResponse echo(String echo) throws McuMgrException {
		HashMap<String, Object> payloadMap = new HashMap<>();
		payloadMap.put("d", echo);
		return send(OP_WRITE, ID_ECHO, payloadMap, McuMgrEchoResponse.class);
	}

	/**
	 * Set the console echo on the device (synchronous).
	 *
	 * @param echo     whether or not to echo to the console
	 * @param callback the asynchronous callback
	 */
	/* TODO: check the response */
	public void consoleEcho(boolean echo, McuMgrCallback<McuMgrEchoResponse> callback) {
		HashMap<String, Object> payloadMap = new HashMap<>();
		payloadMap.put("echo", echo);
		send(OP_WRITE, ID_CONS_ECHO_CTRL, payloadMap, McuMgrEchoResponse.class, callback);
	}

	/**
	 * Set the console echo on the device (synchronous).
	 *
	 * @param echo whether or not to echo to the console
	 * @return the response
	 * @throws McuMgrException Transport error. See cause.
	 */
    /* TODO: check the response */
	public McuMgrEchoResponse consoleEcho(boolean echo) throws McuMgrException {
		HashMap<String, Object> payloadMap = new HashMap<>();
		payloadMap.put("echo", echo);
		return send(OP_WRITE, ID_CONS_ECHO_CTRL, payloadMap, McuMgrEchoResponse.class);
	}

	/**
	 * Get task statistics from the device (asynchronous).
	 *
	 * @param callback the asynchronous callback
	 */
	public void taskstats(McuMgrCallback<McuMgrTaskStatResponse> callback) {
		send(OP_READ, ID_TASKSTATS, null, McuMgrTaskStatResponse.class, callback);
	}

	/**
	 * Get task statistics from the device (synchronous).
	 *
	 * @return the response
	 * @throws McuMgrException Transport error. See cause.
	 */
	public McuMgrTaskStatResponse taskstats() throws McuMgrException {
		return send(OP_READ, ID_TASKSTATS, null, McuMgrTaskStatResponse.class);
	}

	/**
	 * Get memory pool statistics from the device (asynchronous).
	 *
	 * @param callback the asynchronous callback
	 */
	public void mpstat(McuMgrCallback<McuMgrMpStatResponse> callback) {
		send(OP_READ, ID_MPSTATS, null, McuMgrMpStatResponse.class, callback);
	}

	/**
	 * Get memory pool statistics from the device (synchronous).
	 *
	 * @return the response
	 * @throws McuMgrException Transport error. See cause.
	 */
	public McuMgrMpStatResponse mpstat() throws McuMgrException {
		return send(OP_READ, ID_MPSTATS, null, McuMgrMpStatResponse.class);
	}

	/**
	 * Read the date and time on the device (asynchronous).
	 *
	 * @param callback the asynchronous callback
	 */
	public void readDatetime(McuMgrCallback<McuMgrReadDateTimeResponse> callback) {
		send(OP_READ, ID_DATETIME_STR, null, McuMgrReadDateTimeResponse.class, callback);
	}

	/**
	 * Read the date and time on the device (synchronous).
	 *
	 * @return the response
	 * @throws McuMgrException Transport error. See cause.
	 */
	public McuMgrReadDateTimeResponse readDatetime() throws McuMgrException {
		return send(OP_READ, ID_DATETIME_STR, null, McuMgrReadDateTimeResponse.class);
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
    /* TODO: check the response type */
	public void writeDatetime(Date date, TimeZone timeZone, McuMgrCallback<McuMgrReadDateTimeResponse> callback) {
		HashMap<String, Object> payloadMap = new HashMap<>();
		payloadMap.put("datetime", formatDate(date, timeZone));
		send(OP_WRITE, ID_DATETIME_STR, payloadMap, McuMgrReadDateTimeResponse.class, callback);
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
    /* TODO: check the response type */
	public McuMgrReadDateTimeResponse writeDatetime(Date date, TimeZone timeZone) throws McuMgrException {
		HashMap<String, Object> payloadMap = new HashMap<>();
		payloadMap.put("datetime", formatDate(date, timeZone));
		return send(OP_WRITE, ID_DATETIME_STR, payloadMap, McuMgrReadDateTimeResponse.class);
	}

	/**
	 * Reset the device (asynchronous).
	 *
	 * @param callback the asynchronous callback
	 */
	public void reset(McuMgrCallback<McuMgrSimpleResponse> callback) {
		send(OP_WRITE, ID_RESET, null, McuMgrSimpleResponse.class, callback);
	}

	/**
	 * Reset the device (synchronous).
	 *
	 * @return the response
	 * @throws McuMgrException Transport error. See cause.
	 */
	public McuMgrSimpleResponse reset() throws McuMgrException {
		return send(OP_WRITE, ID_RESET, null, McuMgrSimpleResponse.class);
	}
}
