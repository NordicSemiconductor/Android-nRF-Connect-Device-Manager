/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.IOException;
import java.util.Arrays;

import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.McuMgrHeader;
import io.runtime.mcumgr.McuMgrScheme;
import io.runtime.mcumgr.exception.McuMgrCoapException;
import io.runtime.mcumgr.util.CBOR;

@SuppressWarnings({"WeakerAccess", "unused"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class McuMgrResponse {
    private final static String TAG = "McuMgrResponse";

    /**
     * The raw return code found in most McuMgr response payloads. If a rc value is not explicitly
     * stated, a value of 0 is assumed.
     */
    public int rc = 0;

    /**
     * Scheme of the transport which produced this response.
     */
    private McuMgrScheme mScheme;

    /**
     * The bytes of the response packet. This includes the McuMgrHeader for standard schemes and
     * includes the CoAP header for CoAP schemes.
     */
    private byte[] mBytes;

    /**
     * The McuMgrHeader for this response
     */
    private McuMgrHeader mHeader;

    /**
     * The return code (enum) for this response. For the raw return code use the "rc" property.
     */
    private McuMgrErrorCode mRc;

    /**
     * McuMgr payload for this response. This does not include the McuMgr header for standard
     * schemes and does not include the CoAP header for CoAP schemes.
     */
    private byte[] mPayload;

    /**
     * The CoAP Code used for CoAP schemes, formatted as ((class * 100) + detail).
     */
    private int mCoapCode = 0;

    /**
     * Return the string representation of the response payload.
     *
     * @return The string representation of the response payload.
     */
    @Override
    public String toString() {
        try {
            return CBOR.toString(mPayload);
        } catch (IOException e) {
            Log.e(TAG, "Failed to parse response", e);
        }
        return null;
    }

    /**
     * Get the McuMgrHeader for this response.
     *
     * @return The McuMgrHeader.
     */
    public McuMgrHeader getHeader() {
        return mHeader;
    }

    /**
     * Return the Mcu Manager return code as an int.
     *
     * @return Mcu Manager return code.
     */
    public int getRcValue() {
        if (mRc == null) {
            Log.w(TAG, "Response does not contain a McuMgr return code.");
            return 0;
        } else {
            return mRc.value();
        }
    }

    /**
     * Get the return code as an enum.
     *
     * @return The return code enum.
     */
    public McuMgrErrorCode getRc() {
        return mRc;
    }

    /**
     * Returns true if the response payload contains a return code of 0 or no return code. In other
     * words, return true if the command was a success, false otherwise.
     * @return return true if the command was a success, false otherwise
     */
    public boolean isSuccess() {
        return rc == McuMgrErrorCode.OK.value();
    }

    /**
     * Get the response bytes.
     * <p>
     * If using a CoAP scheme this method and {@link McuMgrResponse#getPayload()} will return the
     * same value.
     *
     * @return The response bytes.
     */
    public byte[] getBytes() {
        return mBytes;
    }

    /**
     * Get the response payload bytes.
     * <p>
     * If using a CoAP scheme this method and {@link McuMgrResponse#getBytes()} will return the
     * same value.
     *
     * @return The payload bytes.
     */
    public byte[] getPayload() {
        return mPayload;
    }

    /**
     * Get the scheme used to initialize this response object.
     *
     * @return The scheme.
     */
    public McuMgrScheme getScheme() {
        return mScheme;
    }

    /**
     * Set the return code for CoAP response schemes.
     *
     * @param code The code to set.
     */
    void setCoapCode(int code) {
        mCoapCode = code;
    }

    /**
     * If this response is from a CoAP transport scheme, get the CoAP response code. Otherwise this
     * method will return 0. The code returned from this method should always indicate a successful
     * response because, on error, a McuMgrCoapException will be thrown (triggering the onError
     * callback for asynchronous request).
     *
     * @return The CoAP response code for a CoAP scheme, 0 otherwise.
     */
    public int getCoapCode() {
        return mCoapCode;
    }

    /**
     * Initialize the fields for this response.
     *
     * @param scheme  the scheme.
     * @param bytes   packet bytes.
     * @param header  McuMgrHeader.
     * @param payload McuMgr CBOR payload.
     * @param rc      the return code.
     */
    void initFields(McuMgrScheme scheme, byte[] bytes, McuMgrHeader header, byte[] payload,
                    McuMgrErrorCode rc) {
        mScheme = scheme;
        mBytes = bytes;
        mHeader = header;
        mPayload = payload;
        mRc = rc;
    }

    /**
     * Build a McuMgrResponse.
     *
     * @param scheme the transport scheme used.
     * @param bytes  the response packet's bytes.
     * @param type   the type of response to build.
     * @param <T>    the response type to build.
     * @return The response.
     * @throws IOException              Error parsing response.
     * @throws IllegalArgumentException If the scheme is CoAP.
     */
    public static <T extends McuMgrResponse> T buildResponse(McuMgrScheme scheme, byte[] bytes,
                                                             Class<T> type) throws IOException {
        if (scheme.isCoap()) {
            throw new IllegalArgumentException("Cannot use this method with a CoAP scheme");
        }

        byte[] payload = Arrays.copyOfRange(bytes, McuMgrHeader.HEADER_LENGTH, bytes.length);
        McuMgrHeader header = McuMgrHeader.fromBytes(Arrays.copyOf(bytes, McuMgrHeader.HEADER_LENGTH));

        // Initialize response and set fields
        T response = CBOR.toObject(payload, type);
        McuMgrErrorCode rc = McuMgrErrorCode.valueOf(response.rc);
        response.initFields(scheme, bytes, header, payload, rc);

        return response;
    }


    /**
     * Build a CoAP McuMgrResponse. This method will throw a McuMgrCoapException if the CoAP
     * response code indicates an error.
     *
     * @param scheme     the transport scheme used (should be either COAP_BLE or COAP_UDP).
     * @param bytes      the packet's bytes, including the CoAP header.
     * @param header     the raw McuManager header.
     * @param payload    the raw McuManager payload.
     * @param codeClass  the class of the CoAP response code.
     * @param codeDetail the detail of the CoAP response code.
     * @param type       the type of response to parse the payload into.
     * @param <T>        the type of response to parse the payload into.
     * @return The McuMgrResponse.
     * @throws IOException         if parsing the payload into the object (type T) failed
     * @throws McuMgrCoapException if the CoAP code class indicates a CoAP error response
     */
    public static <T extends McuMgrResponse> T buildCoapResponse(McuMgrScheme scheme, byte[] bytes,
                                                                 byte[] header, byte[] payload,
                                                                 int codeClass, int codeDetail,
                                                                 Class<T> type) throws IOException, McuMgrCoapException {
        // If the code class indicates a CoAP error response, throw a McuMgrCoapException
        if (codeClass == 4 || codeClass == 5) {
            Log.e(TAG, "Received CoAP Error response, throwing McuMgrCoapException");
            throw new McuMgrCoapException(bytes, codeClass, codeDetail);
        }

        T response = CBOR.toObject(payload, type);
        McuMgrErrorCode rc = McuMgrErrorCode.valueOf(response.rc);
        response.initFields(scheme, bytes, McuMgrHeader.fromBytes(header), payload, rc);
        int code = (codeClass * 100) + codeDetail;
        response.setCoapCode(code);
        return response;
    }

    public static boolean requiresDefragmentation(McuMgrScheme scheme, byte[] bytes) throws IOException {
        if (scheme.isCoap()) {
            throw new UnsupportedOperationException("Method not implemented for CoAP");
        } else {
            int expectedLength = getExpectedLength(scheme, bytes);
            return (expectedLength > (bytes.length - McuMgrHeader.HEADER_LENGTH));
        }
    }

    public static int getExpectedLength(McuMgrScheme scheme, byte[] bytes) throws IOException {
        if (scheme.isCoap()) {
            throw new UnsupportedOperationException("Method not implemented for CoAP");
        } else {
            byte[] headerBytes = Arrays.copyOf(bytes, McuMgrHeader.HEADER_LENGTH);
            McuMgrHeader header = McuMgrHeader.fromBytes(headerBytes);
            if (header == null) {
                throw new IOException("Invalid McuMgrHeader");
            }
            return header.getLen() + McuMgrHeader.HEADER_LENGTH;
        }
    }
}

