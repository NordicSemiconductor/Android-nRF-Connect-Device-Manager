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

    private McuMgrScheme mScheme;
    private byte[] mBytes;
    private McuMgrHeader mHeader;
    private McuMgrErrorCode mRc;
    private byte[] mPayload;
    private int mCoapCode = 0;

//    /**
//     * Construct a McuMgrResponse.
//     * <p>
//     * Note: In the case of a CoAP scheme, bytes argument should contain only the CoAP payload,
//     * not the entire CoAP packet (i.e. no CoAP header or header options).
//     *
//     * @param scheme the scheme used by the transporter
//     * @param bytes  the mcu manager response. If using a CoAP scheme these bytes should NOT contain
//     *               the CoAP header and options.
//     * @throws IOException Error parsing response payload into header and return code.
//     */
//    McuMgrResponse(McuMgrScheme scheme, byte[] bytes) throws IOException {
//        mScheme = scheme;
//        mBytes = bytes;
//    }

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

    void initFields(McuMgrScheme scheme, byte[] bytes, McuMgrHeader header, byte[] payload,
                    McuMgrErrorCode rc) {
        mScheme = scheme;
        mBytes = bytes;
        mHeader = header;
        mPayload = payload;
        mRc = rc;

    }

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
     * @param bytes the response in bytes (If using a CoAP scheme, this should NOT include the CoAP
     *              header and options).
     * @return the payload
     */
    public static byte[] parsePayload(McuMgrScheme scheme, byte[] bytes) {
        if (scheme.isCoap()) {
            return bytes;
        } else {
            return Arrays.copyOfRange(bytes, McuMgrHeader.NMGR_HEADER_LEN, bytes.length);
        }
    }

    /**
     * Build an McuMgrResponse.
     * @param scheme the transport scheme used
     * @param bytes the
     * @param type
     * @param <T>
     * @return
     * @throws IOException
     */
    public static <T extends McuMgrResponse> T buildResponse(McuMgrScheme scheme, byte[] bytes,
                                                             Class<T> type) throws IOException {
        byte[] payload = parsePayload(scheme, bytes);
        T response = CBOR.toObject(payload, type);
        McuMgrHeader header = parseHeader(scheme, bytes);
        McuMgrErrorCode rc = McuMgrErrorCode.valueOf(response.rc);
        response.initFields(scheme, bytes, header, payload, rc);
        return response;
    }

    /**
     * Parses a Coap scheme response
     * @param scheme
     * @param bytes
     * @param type
     * @param <T>
     * @return
     * @throws IOException
     */
    public static <T extends McuMgrResponse> T buildCoapResponse(McuMgrScheme scheme, byte[] bytes,
                                                                 Class<T> type)
        throws IOException {
        int code = CoapUtil.getCode(bytes);
        byte[] payload = CoapUtil.getPayload(bytes);
        T response = buildResponse(scheme, payload, type);
        response.setCoapCode(code);
        return response;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CoapHeaderResponse {
        public byte[] _h;
    }
}

