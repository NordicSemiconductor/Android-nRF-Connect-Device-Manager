package io.runtime.mcumgr;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.mgrs.ConfigManager;
import io.runtime.mcumgr.mgrs.DefaultManager;
import io.runtime.mcumgr.mgrs.ImageManager;
import io.runtime.mcumgr.mgrs.LogManager;
import io.runtime.mcumgr.mgrs.StatsManager;
import io.runtime.mcumgr.util.CBOR;

/**
 * TODO
 */
public abstract class McuManager {

    /**
     * This manager's group ID
     */
    private int mGroupId;

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
    public void send(byte[] data, McuMgrCallback callback) {
        mTransporter.send(data, callback);
    }

    /**
     * Send data synchronously using the transporter.
     *
     * @param data the data to send
     * @throws McuMgrException when an error occurs while sending the data.
     */
    public void send(byte[] data) throws McuMgrException {
        mTransporter.send(data);
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
    public void send(int op, int commandId, Map<String, Object> payloadMap, McuMgrCallback callback) {
        send(op, 0, 0, mGroupId, 0, commandId, payloadMap, callback);
    }

    /**
     * Send an asynchronous Newt Manager command.
     * <p>
     * Additionally builds the Newt Manager header and formats the packet based on scheme before
     * sending it to the transporter.
     *
     * @param op          the operation ({@link McuManager#OP_READ}, {@link McuManager#OP_WRITE})
     * @param flags       additional flags
     * @param len         length
     * @param groupId     group ID of the command
     * @param sequenceNum sequence number
     * @param commandId   ID of the command in the group
     * @param payloadMap  map of command's key-value pairs to construct a CBOR payload
     * @param callback    asynchronous callback
     */
    public void send(int op, int flags, int len, int groupId, int sequenceNum, int commandId,
                     Map<String, Object> payloadMap, McuMgrCallback callback) {
        try {
            byte[] header = McuMgrHeader.build(op, flags, len, groupId, sequenceNum, commandId);
            byte[] payload = buildPayload(header, payloadMap);
            mTransporter.send(payload, callback);
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
     * @param op         the operation ({@link McuManager#OP_READ}, {@link McuManager#OP_WRITE})
     * @param commandId  the ID of the command
     * @param payloadMap the map of values to send along. This argument can be null if the header is
     *                   the only required field.
     * @return The McuMgrResponse or null if an error occurred
     */
    public McuMgrResponse send(int op, int commandId, Map<String, Object> payloadMap)
            throws McuMgrException {
        return send(op, 0, 0, mGroupId, 0, commandId, payloadMap);
    }

    /**
     * Send synchronous Newt Manager command.
     * <p>
     * Additionally builds the Newt Manager header and formats the packet based on scheme before
     * sending it to the transporter.
     *
     * @param op          the operation ({@link McuManager#OP_READ}, {@link McuManager#OP_WRITE})
     * @param flags       additional flags
     * @param len         length
     * @param groupId     Group ID of the command
     * @param sequenceNum sequence number
     * @param commandId   ID of the command in the group
     * @param payloadMap  Map of
     * @return the newt manager response
     * @throws McuMgrException on transport error. See exception cause for more info.
     */
    public McuMgrResponse send(int op, int flags, int len, int groupId, int sequenceNum,
                               int commandId, Map<String, Object> payloadMap)
            throws McuMgrException {
        try {
            byte[] header = McuMgrHeader.build(op, flags, len, groupId, sequenceNum, commandId);
            byte[] payload = buildPayload(header, payloadMap);
            return mTransporter.send(payload);
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
        if (getScheme() == Scheme.COAP_BLE || getScheme() == Scheme.COAP_UDP) {
            if (payloadMap == null) {
                payloadMap = new HashMap<>();
            }
            // CoAP Scheme puts the header as a key-value pair in the payload
            if (payloadMap.get(HEADER_KEY) == null) {
                payloadMap.put(HEADER_KEY, header);
            }
            payload = CBOR.toBytes(payloadMap);
        } else if (payloadMap == null) {
            // Standard scheme with no payload map means our payload is just the header
            payload = header;
        } else {
            // Standard scheme appends the CBOR payload to the header.
            byte[] cborPayload = CBOR.toBytes(payloadMap);
            payload = new byte[header.length + cborPayload.length];
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
    public Scheme getScheme() {
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

    //******************************************************************
    // Scheme
    //******************************************************************

    /**
     * Newt Manager transport scheme.
     * <p>
     * Newt Manager supports four transport schemes: BLE, CoAP BLE, UDP, and CoAP UDP.
     */
    public enum Scheme {
        BLE,
        COAP_BLE,
        UDP,
        COAP_UDP;

        public boolean isCoap() {
            return (this == COAP_BLE || this == COAP_UDP);
        }
    }

    //******************************************************************
    // Code
    //******************************************************************

    public enum Code {
        OK(0),
        UNKNOWN(1),
        NO_MEMORY(2),
        IN_VALUE(3),
        TIMEOUT(4),
        NO_ENTRY(5),
        BAD_STATE(6);

        private int mCode;

        Code(int code) {
            mCode = code;
        }

        public int value() {
            return mCode;
        }

        @Override
        public String toString() {
            return "NewtMgrError: " + super.toString() + "(" + mCode + ")";
        }

        public static Code valueOf(int error) {
            switch (error) {
                case 0:
                    return OK;
                case 1:
                    return UNKNOWN;
                case 2:
                    return NO_MEMORY;
                case 3:
                    return IN_VALUE;
                case 4:
                    return TIMEOUT;
                case 5:
                    return NO_ENTRY;
                case 6:
                    return BAD_STATE;
                default:
                    return null;
            }
        }
    }

    //******************************************************************
    // Constants
    //******************************************************************

    // CoAP Newt Manager Constants
    public final static String COAP_URI = "/omgr";
    public final static String HEADER_KEY = "_h";

    // Newt Manager operation codes
    public final static int OP_READ = 0;
    public final static int OP_READ_RSP = 1;
    public final static int OP_WRITE = 2;
    public final static int OP_WRITE_RSP = 3;

    // Newt Manager groups
    public final static int GROUP_DEFAULT = 0;
    public final static int GROUP_IMAGE = 1;
    public final static int GROUP_STATS = 2;
    public final static int GROUP_CONFIG = 3;
    public final static int GROUP_LOGS = 4;
    public final static int GROUP_CRASH = 5;
    public final static int GROUP_PERUSER = 64;
}
