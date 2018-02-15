package io.runtime.mcumgr;

import android.support.annotation.Nullable;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import io.runtime.mcumgr.exception.McuMgrException;
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
     * Send an asynchronous Newt Manager command.
     * <p>
     * Additionally builds the Newt Manager header and formats the packet based on scheme before
     * sending it to the transporter.
     *
     * @param op          The operation ({@link McuManager#OP_READ}, {@link McuManager#OP_WRITE})
     * @param flags       Additional flags
     * @param len         Packet length. If this argument is zero, the length will be calculated
     *                    and set automatically.
     * @param groupId     Group ID of the command
     * @param sequenceNum Sequence number
     * @param commandId   ID of the command in the group
     * @param payloadMap  Map of payload key-value pairs
     * @param callback    Asynchronous callback
     */
    public void send(int op, int flags, int len, int groupId, int sequenceNum, int commandId,
                     Map<String, Object> payloadMap, McuMgrCallback callback) {
        try {
            byte[] packet = buildPacket(op, flags, len, groupId, sequenceNum, commandId, payloadMap);
            send(packet, callback);
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
     * @param op          The operation ({@link McuManager#OP_READ}, {@link McuManager#OP_WRITE})
     * @param flags       Additional flags
     * @param len         Packet length. If this argument is zero, the length will be calculated
     *                    and set automatically.
     * @param groupId     Group ID of the command
     * @param sequenceNum Sequence number
     * @param commandId   ID of the command in the group
     * @param payloadMap  Map of payload key-value pairs
     * @return the newt manager response
     * @throws McuMgrException on transport error. See exception cause for more info.
     */
    public McuMgrResponse send(int op, int flags, int len, int groupId, int sequenceNum,
                               int commandId, Map<String, Object> payloadMap)
            throws McuMgrException {
        byte[] packet = buildPacket(op, flags, len, groupId, sequenceNum, commandId, payloadMap);
        return send(packet);
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
    public McuMgrResponse send(byte[] data) throws McuMgrException {
        return mTransporter.send(data);
    }

    /**
     * Build a Mcu Manager packet based on the transport scheme.
     *
     * @param op          The operation ({@link McuManager#OP_READ}, {@link McuManager#OP_WRITE})
     * @param flags       Additional flags
     * @param len         Packet length. If this argument is zero, the length will be calculated
     *                    and set automatically.
     * @param groupId     Group ID of the command
     * @param sequenceNum Sequence number
     * @param commandId   ID of the command in the group
     * @param payloadMap  Map of payload key-value pairs
     * @return the packet data
     */
    public byte[] buildPacket(int op, int flags, int len, int groupId, int sequenceNum,
                              int commandId, @Nullable Map<String, Object> payloadMap)
            throws McuMgrException {
        byte[] packet;
        try {
            // If the input length is zero set the McuMgrHeader length field.
            if (len == 0 && payloadMap != null) {
                // Copy the payload map
                HashMap<String, Object> payloadMapCopy = new HashMap<>(payloadMap);
                // Remove the header if present
                payloadMapCopy.remove(HEADER_KEY);
                len = CBOR.toBytes(payloadMapCopy).length;
            }

            // Build header
            byte[] header = McuMgrHeader.build(op, flags, len, groupId, sequenceNum, commandId);

            // Build the packet based on scheme
            if (getScheme() == Scheme.COAP_BLE || getScheme() == Scheme.COAP_UDP) {
                if (payloadMap == null) {
                    payloadMap = new HashMap<>();
                }
                // CoAP Scheme puts the header as a key-value pair in the payload
                if (payloadMap.get(HEADER_KEY) == null) {
                    payloadMap.put(HEADER_KEY, header);
                }
                packet = CBOR.toBytes(payloadMap);
            } else if (payloadMap == null) {
                // Standard scheme with no payload map means our payload is just the header
                packet = header;
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
        BAD_STATE(6),
        MSG_SIZE(7),
        NOT_SUPPORTED(8),
        PER_USER(256);

        private int mCode;

        Code(int code) {
            mCode = code;
        }

        public int value() {
            return mCode;
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
                case 7:
                    return MSG_SIZE;
                case 8:
                    return NOT_SUPPORTED;
                case 256:
                    return PER_USER;
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
