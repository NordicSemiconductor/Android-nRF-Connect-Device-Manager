/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr;

import android.support.annotation.Nullable;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.util.CBOR;

/**
 * TODO
 */
public abstract class McuManager {

    private static final String TAG = McuManager.class.getSimpleName();

    // Date format
    public final static String MCUMGR_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZZZZZ";

    // CoAP Constants
    public final static String COAP_URI     = "/omgr";
    public final static String HEADER_KEY   = "_h";

    // Newt Manager operation codes
    public final static int OP_READ         = 0;
    public final static int OP_READ_RSP     = 1;
    public final static int OP_WRITE        = 2;
    public final static int OP_WRITE_RSP    = 3;

    // Newt Manager groups
    public final static int GROUP_DEFAULT   = 0;
    public final static int GROUP_IMAGE     = 1;
    public final static int GROUP_STATS     = 2;
    public final static int GROUP_CONFIG    = 3;
    public final static int GROUP_LOGS      = 4;
    public final static int GROUP_CRASH     = 5;
    public final static int GROUP_SPLIT     = 6;
    public final static int GROUP_RUN       = 7;
    public final static int GROUP_FS        = 8;
    public final static int GROUP_PERUSER   = 64;

    /**
     * This manager's group ID
     */
    private final int mGroupId;

    /**
     * Handles sending the Newt Manager command data over the transport specified by its scheme.
     */
    private McuMgrTransport mTransporter;

    /**
     * Construct a McuManager instance.
     *
     * @param groupId     The group ID of this Newt Manager instance
     * @param transporter The transporter to use to send commands
     */
    protected McuManager(int groupId, McuMgrTransport transporter) {
        mGroupId = groupId;
        mTransporter = transporter;
    }

    /**
     * Get the group ID for this manager.
     *
     * @return the group ID for this manager.
     */
    public int getGroupId() {
        return mGroupId;
    }

    /**
     * Get the transporter's scheme
     *
     * @return the transporter's scheme
     */
    public McuMgrScheme getScheme() {
        return mTransporter.getScheme();
    }

    /**
     * Get the transporter
     *
     * @return transporter for this new manager instance
     */
    public McuMgrTransport getTransporter() {
        return mTransporter;
    }

    /**
     * Send an asynchronous Newt Manager command.
     * <p>
     * Additionally builds the Newt Manager header and formats the packet based on scheme before
     * sending it to the transporter.
     *
     * @param op         the operation ({@link McuManager#OP_READ}, {@link McuManager#OP_WRITE})
     * @param commandId  the ID of the command
     * @param payloadMap the map of values to send along. This argument can be null if the header is
     *                   the only required field.
     * @param callback   the response callback
     */
    public <T extends McuMgrResponse> void send(int op, int commandId, Map<String, Object> payloadMap,
                                                Class<T> respType, McuMgrCallback<T> callback) {
        send(op, 0, mGroupId, 0, commandId, payloadMap, respType, callback);
    }

    /**
     * Send synchronous Newt Manager command.
     * <p>
     * Additionally builds the Newt Manager header and formats the packet based on scheme before
     * sending it to the transporter.
     *
     * @param op         the operation ({@link McuManager#OP_READ}, {@link McuManager#OP_WRITE})
     * @param commandId  the ID of the command
     * @param payloadMap the map of values to send along. This argument can be null if the header is
     *                   the only required field.
     * @return The McuMgrResponse or null if an error occurred
     */
    public <T extends McuMgrResponse> T send(int op, int commandId, Map<String, Object> payloadMap,
                                             Class<T> respType)
            throws McuMgrException {
        return send(op, 0, mGroupId, 0, commandId, respType, payloadMap);
    }

    /**
     * Send an asynchronous Newt Manager command.
     * <p>
     * Additionally builds the Newt Manager header and formats the packet based on scheme before
     * sending it to the transporter.
     *
     * @param op          the operation ({@link McuManager#OP_READ}, {@link McuManager#OP_WRITE})
     * @param flags       additional flags
     * @param groupId     group ID of the command
     * @param sequenceNum sequence number
     * @param commandId   ID of the command in the group
     * @param payloadMap  map of command's key-value pairs to construct a CBOR payload
     * @param callback    asynchronous callback
     */
    public <T extends McuMgrResponse> void send(int op, int flags, int groupId, int sequenceNum, int
            commandId, Map<String, Object> payloadMap, Class<T> respType, McuMgrCallback<T> callback) {
        try {
            byte[] packet = buildPacket(op, flags, groupId, sequenceNum, commandId, payloadMap);
            send(packet, respType, callback);
        } catch (McuMgrException e) {
            callback.onError(e);
        }
    }

    /**
     * Send synchronous Newt Manager command.
     * <p>
     * Additionally builds the Newt Manager header and formats the packet based on scheme before
     * sending it to the transporter.
     *
     * @param op          The operation ({@link McuManager#OP_READ},
     *                      {@link McuManager#OP_WRITE})
     * @param flags       Additional flags
     * @param groupId     Group ID of the command
     * @param sequenceNum Sequence number
     * @param commandId   ID of the command in the group
     * @param payloadMap  Map of payload key-value pairs
     * @return the newt manager response
     * @throws McuMgrException on transport error. See exception cause for more info.
     */
    public <T extends McuMgrResponse> T send(int op, int flags, int groupId, int sequenceNum,
                                             int commandId, Class<T> respType,
                                             Map<String, Object> payloadMap)
            throws McuMgrException {
        byte[] packet = buildPacket(op, flags, groupId, sequenceNum, commandId, payloadMap);
        return send(packet, respType);
    }

    /**
     * Send data asynchronously using the transporter.
     *
     * @param data     the data to send
     * @param callback the response callback
     */
    public <T extends McuMgrResponse> void send(byte[] data, Class<T> respType,
                                                McuMgrCallback<T> callback) {
        mTransporter.send(data, respType, callback);
    }

    /**
     * Send data synchronously using the transporter.
     *
     * @param data the data to send
     * @throws McuMgrException when an error occurs while sending the data.
     */
    public <T extends McuMgrResponse> T send(byte[] data, Class<T> respType)
            throws McuMgrException {
        return mTransporter.send(data, respType);
    }

    /**
     * Build a Mcu Manager packet based on the transport scheme.
     *
     * @param op          The operation ({@link McuManager#OP_READ},
     *                    {@link McuManager#OP_WRITE})
     * @param flags       Additional flags
     * @param groupId     Group ID of the command
     * @param sequenceNum Sequence number
     * @param commandId   ID of the command in the group
     * @param payloadMap  Map of payload key-value pairs
     * @return the packet data
     */
    public byte[] buildPacket(int op, int flags, int groupId, int sequenceNum,
                              int commandId, @Nullable Map<String, Object> payloadMap)
            throws McuMgrException {
        byte[] packet;
        try {
            // If the payload map is null initialize an empty payload map
            if (payloadMap == null) {
                payloadMap = new HashMap<>();
            }

            // Copy the payload map to remove the header key
            HashMap<String, Object> payloadMapCopy = new HashMap<>(payloadMap);
            // Remove the header if present (for CoAP schemes)
            payloadMapCopy.remove(HEADER_KEY);

            // Get the length
            int len = CBOR.toBytes(payloadMapCopy).length;

            // Build header
            byte[] header = McuMgrHeader.build(op, flags, len, groupId, sequenceNum, commandId);

            // Build the packet based on scheme
            if (getScheme() == McuMgrScheme.COAP_BLE || getScheme() == McuMgrScheme.COAP_UDP) {
                // CoAP Scheme puts the header as a key-value pair in the payload
                if (payloadMap.get(HEADER_KEY) == null) {
                    payloadMap.put(HEADER_KEY, header);
                }
                packet = CBOR.toBytes(payloadMap);
            } else {
                // Standard scheme appends the CBOR payload to the header.
                byte[] cborPayload = CBOR.toBytes(payloadMap);
                packet = new byte[header.length + cborPayload.length];
                System.arraycopy(header, 0, packet, 0, header.length);
                System.arraycopy(cborPayload, 0, packet, header.length, cborPayload.length);
            }
        } catch (IOException e) {
            throw new McuMgrException("An error occurred serializing CBOR payload", e);
        }
        return packet;
    }

    //******************************************************************
    // Utilities
    //******************************************************************

    /**
     * Format a Date and a TimeZone into a String which Newt Manager will accept.
     *
     * @param date     The date to format. If null, the current date on the device will be used.
     * @param timeZone The timezone of the given date. If null, the timezone on the device will be used.
     * @return A formatted string of the provided date and timezone
     */
    public static String dateToString(Date date, TimeZone timeZone) {
        if (date == null) {
            date = new Date();
        }
        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }
        SimpleDateFormat mcumgrFormat = new SimpleDateFormat(MCUMGR_DATE_FORMAT,
                new Locale("US"));
        mcumgrFormat.setTimeZone(timeZone);
        return mcumgrFormat.format(date);
    }

    /**
     * Parse a date string returned by a McuMgr response
     * @param dateString the string to parse
     * @return the Date of the string, null on error
     */
    public static Date stringToDate(String dateString) {
        if (dateString == null) {
            return null;
        }
        SimpleDateFormat mcumgrFormat = new SimpleDateFormat(MCUMGR_DATE_FORMAT,
                new Locale("US"));
        try {
            return mcumgrFormat.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}
