package io.runtime.mcumgr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.runtime.mcumgr.util.CBOR;

public abstract class McuMgrResponse {

    private McuManager.Scheme mScheme;
    private byte[] mBytes;
    private McuMgrHeader mHeader;
    private int mRc = -1;
    private byte[] mPayload;

    /**
     * Construct a McuMgrResponse.
     *
     * Note: In the case of a CoAP scheme, bytes argument should contain only the CoAP payload,
     *       not the entire CoAP packet (i.e. no CoAP header or header options).
     * @param scheme the scheme used by the transporter
     * @param bytes the mcu manager response. If using a CoAP scheme these bytes should NOT contain
     *              the CoAP header and options.
     * @throws IOException Error parsing response payload into header and return code.
     */
    public McuMgrResponse(McuManager.Scheme scheme, byte[] bytes) throws IOException {
        mScheme = scheme;
        mBytes = bytes;
        mHeader = parseHeader(scheme, bytes);
        mPayload = parsePayload(scheme, bytes);
        mRc = parseRc(mPayload);
    }

    /**
     * Used primarily for a CoAP schemes to indicate a CoAP response error.
     *
     * @return
     */
    public abstract boolean isSuccess();

    public McuMgrHeader getHeader() {
        return mHeader;
    }
    public int getRcValue() {
        return mRc;
    }
    public McuManager.Code getRc() {
        return McuManager.Code.valueOf(mRc);
    }
    public byte[] getBytes() {
        return mBytes;
    }
    public byte[] getPayload() {
        return mPayload;
    }
    public McuManager.Scheme getScheme() {
        return mScheme;
    }

    public static McuMgrHeader parseHeader(McuManager.Scheme scheme, byte[] bytes)
            throws IOException{
        if (scheme.isCoap()) {
            CoapBaseResponse response = CBOR.toObject(bytes, CoapBaseResponse.class);
            return McuMgrHeader.fromBytes(response._h);
        } else {
            byte[] header = Arrays.copyOf(bytes, McuMgrHeader.NMGR_HEADER_LEN);
            return McuMgrHeader.fromBytes(header);
        }
    }

    public static byte[] parsePayload(McuManager.Scheme scheme, byte[] bytes) {
        if (scheme.isCoap()) {
            return bytes;
        } else {
            return Arrays.copyOfRange(bytes, McuMgrHeader.NMGR_HEADER_LEN, bytes.length);
        }
    }

    public static int parseRc(byte[] payload) throws IOException {
        return CBOR.toObject(payload, BaseResponse.class).rc;
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class BaseResponse {
        public int rc;
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class CoapBaseResponse {
        public byte[] _h;
        public int rc;
    }
}
