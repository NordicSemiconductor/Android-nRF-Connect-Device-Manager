/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr;

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.resp.McuMgrResponse;
import io.runtime.mcumgr.util.ByteUtil;
import io.runtime.mcumgr.util.CBOR;

import static io.runtime.mcumgr.McuMgrConstants.HEADER_KEY;

/**
 * TODO
 */
public abstract class McuManager {

    private static final String TAG = McuManager.class.getSimpleName();

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
     * Send data asynchronously using the transporter.
     *
     * @param data     the data to send
     * @param callback the response callback
     */
    public void send(byte[] data, Class<? extends McuMgrResponse> respType, McuMgrCallback callback) {
        mTransporter.send(data, respType, callback);
    }

    /**
     * Send data synchronously using the transporter.
     *
     * @param data the data to send
     * @throws McuMgrException when an error occurs while sending the data.
     */
    public void send(byte[] data, Class<? extends McuMgrResponse> respType) throws McuMgrException {
        mTransporter.send(data, respType);
    }

    /**
     * Send an asynchronous Newt Manager command.
     * <p>
     * Additionally builds the Newt Manager header and formats the packet based on scheme before
     * sending it to the transporter.
     *
     * @param op         the operation ({@link McuMgrConstants#OP_READ}, {@link McuMgrConstants#OP_WRITE})
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
     * Send an asynchronous Newt Manager command.
     * <p>
     * Additionally builds the Newt Manager header and formats the packet based on scheme before
     * sending it to the transporter.
     *
     * @param op          the operation ({@link McuMgrConstants#OP_READ}, {@link McuMgrConstants#OP_WRITE})
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
            int len;

            if (!getScheme().isCoap()) {
                len = CBOR.toBytes(payloadMap).length;
            } else {
                /* TODO for COAP */
                len = 0;
            }

            byte[] header = McuMgrHeader.build(op, flags, len, groupId, sequenceNum, commandId);
            byte[] payload = buildPayload(header, payloadMap);
            mTransporter.send(payload, respType, callback);
        } catch (IOException e) {
            callback.onError(new McuMgrException("An error occurred serializing CBOR payload"));
        }
    }

    /**
     * Send synchronous Newt Manager command.
     * <p>
     * Additionally builds the Newt Manager header and formats the packet based on scheme before
     * sending it to the transporter.
     *
     * @param op         the operation ({@link McuMgrConstants#OP_READ}, {@link McuMgrConstants#OP_WRITE})
     * @param commandId  the ID of the command
     * @param payloadMap the map of values to send along. This argument can be null if the header is
     *                   the only required field.
     * @return The McuMgrResponse or null if an error occurred
     */
    public <T extends McuMgrResponse> T send(int op, int commandId, Map<String, Object> payloadMap,
                                             Class<T> respType)
            throws McuMgrException {
        return send(op, 0, 0, mGroupId, 0, commandId, respType, payloadMap);
    }

    /**
     * Send synchronous Newt Manager command.
     * <p>
     * Additionally builds the Newt Manager header and formats the packet based on scheme before
     * sending it to the transporter.
     *
     * @param op          the operation ({@link McuMgrConstants#OP_READ}, {@link McuMgrConstants#OP_WRITE})
     * @param flags       additional flags
     * @param len         length
     * @param groupId     Group ID of the command
     * @param sequenceNum sequence number
     * @param commandId   ID of the command in the group
     * @param payloadMap  Map of
     * @return the newt manager response
     * @throws McuMgrException on transport error. See exception cause for more info.
     */
    public <T extends McuMgrResponse> T send(int op, int flags, int len, int groupId, int sequenceNum,
                                             int commandId, Class<T> respType, Map<String, Object> payloadMap)
            throws McuMgrException {
        try {
            if (len == 0 && !getScheme().isCoap()) {
                len = CBOR.toBytes(payloadMap).length;
            }
            byte[] header = McuMgrHeader.build(op, flags, len, groupId, sequenceNum, commandId);
            byte[] payload = buildPayload(header, payloadMap);
            Log.d(TAG, "Sending " + ByteUtil.byteArrayToHex(payload, "0x%02X "));
            return mTransporter.send(payload, respType);
        } catch (IOException e) {
            throw new McuMgrException("An error occurred serializing CBOR payload", e);
        }
    }

    /**
     * Builds a payload for a command based on this manager's transport scheme.
     *
     * @param header     the header of this command
     * @param payloadMap the map of key-value pairs to CBOR encode
     * @return the payload to send over the transporter
     * @throws IOException Error CBOR encoding payload map
     */
    public byte[] buildPayload(byte[] header, Map<String, Object> payloadMap) throws IOException {
        byte[] payload;
        if (getScheme() == McuMgrScheme.COAP_BLE || getScheme() == McuMgrScheme.COAP_UDP) {
            if (payloadMap == null) {
                payloadMap = new HashMap<>();
            }
            // CoAP Scheme puts the header as a key-value pair in the payload
            if (payloadMap.get(HEADER_KEY) == null) {
                payloadMap.put(HEADER_KEY, header);
            }
            payload = CBOR.toBytes(payloadMap);
        } else if (payloadMap == null) {
            // Standard scheme with no payload map means our payload is just the header + 0x0a byte !!!
            /* TODO: remove the 0x0a once it has been fixed in the embedded part */
            payload = new byte[header.length + 1];
            System.arraycopy(header, 0, payload, 0, header.length);
            payload[header.length] = 0x0A;
        } else {
            // Standard scheme appends the CBOR payload to the header.
            byte[] cborPayload = CBOR.toBytes(payloadMap);
            payload = new byte[header.length + cborPayload.length];
            ByteBuffer.wrap(header).putShort(2, (short) cborPayload.length).array();
            System.arraycopy(header, 0, payload, 0, header.length);
            System.arraycopy(cborPayload, 0, payload, header.length, cborPayload.length);

        }
        return payload;
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
    public static String formatDate(Date date, TimeZone timeZone) {
        if (date == null) {
            date = new Date();
        }
        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }
        SimpleDateFormat nmgrFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ",
                new Locale("US"));
        nmgrFormat.setTimeZone(timeZone);
        return nmgrFormat.format(date);
    }
}
