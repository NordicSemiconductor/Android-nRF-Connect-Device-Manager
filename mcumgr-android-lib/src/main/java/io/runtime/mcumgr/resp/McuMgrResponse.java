/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.resp;

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
     * Parse the header from a response.
     * @param scheme the response's scheme
     * @param bytes the response in bytes (If using a CoAP scheme, this should NOT include the CoAP
     *              header and options).
     * @return the header
     * @throws IOException Error parsing the bytes into an object
     */
    public static McuMgrHeader parseHeader(McuMgrScheme scheme, byte[] bytes)
            throws IOException {
        if (scheme.isCoap()) {
            CoapHeaderResponse response = CBOR.toObject(bytes, CoapHeaderResponse.class);
            return McuMgrHeader.fromBytes(response._h);
        } else {
            byte[] header = Arrays.copyOf(bytes, McuMgrHeader.NMGR_HEADER_LEN);
            return McuMgrHeader.fromBytes(header);
        }
    }

    /**
     * Parse the payload from a response.
     * @param scheme the response's scheme
     * @param bytes the response in bytes
     * @return the payload
     */
    public static byte[] parsePayload(McuMgrScheme scheme, byte[] bytes) throws IOException {
        if (scheme.isCoap()) {
            return CoapUtil.getPayload(bytes);
        } else {
            return Arrays.copyOfRange(bytes, McuMgrHeader.NMGR_HEADER_LEN, bytes.length);
        }
    }

    /**
     * Build an McuMgrResponse.
     * @param scheme the transport scheme used
     * @param bytes the response packet's bytes
     * @param type the type of response to build
     * @param <T> The response type to build
     * @return The response
     * @throws IOException Error parsing response
     */
    public static <T extends McuMgrResponse> T buildResponse(McuMgrScheme scheme, byte[] bytes,
                                                             Class<T> type) throws IOException {
        // Parse header and payload
        McuMgrHeader header = parseHeader(scheme, bytes);
        byte[] payload = parsePayload(scheme, bytes);

        // Initialize response and set fields
        T response = CBOR.toObject(payload, type);
        McuMgrErrorCode rc = McuMgrErrorCode.valueOf(response.rc);
        response.initFields(scheme, bytes, header, payload, rc);
        return response;
    }

    /**
     * Parses a Coap scheme response
     * @param scheme The transport scheme used
     * @param bytes The response packet's bytes. This inlcudes the CoAP header and options
     * @param type The type of response to build and return
     * @param <T> The type of response to build and return
     * @return The response
     * @throws IOException Error parsing response
     */
    public static <T extends McuMgrResponse> T buildCoapResponse(McuMgrScheme scheme, byte[] bytes,
                                                                 Class<T> type) throws IOException {
        // Build a response and set the CoAP Response Code
        T response = buildResponse(scheme, bytes, type);
        response.setCoapCode(CoapUtil.getCode(bytes));
        return response;
    }

    /**
     * POJO for obtaining the McuMgrHeader from a CoAP response payload
     */
    private static class CoapHeaderResponse {
        public byte[] _h;
    }
}

