/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.msg;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.IOException;
import java.util.Arrays;

import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.McuMgrHeader;
import io.runtime.mcumgr.McuMgrScheme;
import io.runtime.mcumgr.util.CBOR;
import io.runtime.mcumgr.util.CoapUtil;

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
     * @return the string representation of the response payload.
     */
    @Override
    public String toString() {
        try {
            return CBOR.toString(mPayload);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get the McuMgrHeader for this response
     *
     * @return the McuMgrHeader
     */
    public McuMgrHeader getHeader() {
        return mHeader;
    }

    /**
     * Return the Mcu Manager return code as an int
     *
     * @return Mcu Manager return code
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
     * Get the return code as an enum
     *
     * @return the return code enum
     */
    public McuMgrErrorCode getRc() {
        return mRc;
    }

    /**
     * Get the response bytes.
     * <p>
     * If using a CoAP scheme this method and {@link McuMgrResponse#getPayload()} will return the
     * same value.
     *
     * @return the response bytes
     */
    public byte[] getBytes() {
        return mBytes;
    }

    /**
     * Get the response payload in bytes.
     * <p>
     * If using a CoAP scheme this method and {@link McuMgrResponse#getPayload()} will return the
     * same value.
     *
     * @return the payload bytes
     */
    public byte[] getPayload() {
        return mPayload;
    }

    /**
     * Get the scheme used to initialize this response object.
     * @return the scheme
     */
    public McuMgrScheme getScheme() {
        return mScheme;
    }

    /**
     * Used primarily for a CoAP schemes to indicate a CoAP response error.
     *
     * @return true if a Mcu Manager response was received successfully (i.e. no CoAP error), false
     * otherwise.
     */
    public boolean isSuccess() {
        if (mScheme.isCoap()) {
            return mCoapCode >= 200 && mCoapCode < 300;
        } else {
            return true;
        }
    }

    /**
     * Initialize the fields for this response.
     * @param scheme the scheme
     * @param bytes packet bytes
     * @param header McuMgrHeader
     * @param payload McuMgr CBOR payload
     * @param rc the return code
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
     * Set the return code for CoAP response schemes.
     * @param code
     */
    void setCoapCode(int code) {
        mCoapCode = code;
    }

    /**
     * Build a McuMgrResponse.
     * @param scheme the transport scheme used
     * @param bytes the response packet's bytes
     * @param type the type of response to build
     * @param <T> The response type to build
     * @return The response
     * @throws IOException Error parsing response
     */
    public static <T extends McuMgrResponse> T buildResponse(McuMgrScheme scheme, byte[] bytes,
                                                             Class<T> type) throws IOException {
        McuMgrHeader header;
        byte[] payload;
        // Parse the McuMgrHeader and payload based on scheme
        if (scheme.isCoap()) {
            payload = CoapUtil.getPayload(bytes);
            CoapHeaderResponse response = CBOR.toObject(payload, CoapHeaderResponse.class);
            header = McuMgrHeader.fromBytes(response._h);
        } else {
            payload = Arrays.copyOfRange(bytes, McuMgrHeader.NMGR_HEADER_LEN, bytes.length);
            header = McuMgrHeader.fromBytes(Arrays.copyOf(bytes, McuMgrHeader.NMGR_HEADER_LEN));
        }

        // Initialize response and set fields
        T response = CBOR.toObject(payload, type);
        McuMgrErrorCode rc = McuMgrErrorCode.valueOf(response.rc);
        response.initFields(scheme, bytes, header, payload, rc);

        // If we are using a CoAP scheme, parse the CoAP response code
        if (scheme.isCoap()) {
            response.setCoapCode(CoapUtil.getCode(bytes));
        }
        return response;
    }

    public static boolean requiresDefragmentation(McuMgrScheme scheme, byte[] bytes) throws IOException {
        int expectedLength = getExpectedLength(scheme, bytes);
        if (scheme.isCoap()) {
            throw new RuntimeException("Method not implemented for coap");
        } else {
            return (expectedLength > (bytes.length - McuMgrHeader.NMGR_HEADER_LEN));
        }
    }

    public static int getExpectedLength(McuMgrScheme scheme, byte[] bytes) throws IOException {
        if (scheme.isCoap()) {
            throw new RuntimeException("Method not implemented for coap");
        } else {
            byte[] headerBytes = Arrays.copyOf(bytes, McuMgrHeader.NMGR_HEADER_LEN);
            McuMgrHeader header = McuMgrHeader.fromBytes(headerBytes);
            if (header == null) {
                throw new IOException("Invalid McuMgrHeader");
            }
            return header.getLen() + McuMgrHeader.NMGR_HEADER_LEN;
        }
    }

    /**
     * POJO for obtaining the McuMgrHeader from a CoAP response payload
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CoapHeaderResponse {
        public byte[] _h;
    }
}

