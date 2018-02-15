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

package io.runtime.mcumgr.resp;

import android.util.Log;

import java.io.IOException;
import java.util.Arrays;

import io.runtime.mcumgr.McuMgrHeader;
import io.runtime.mcumgr.McuMgrScheme;
import io.runtime.mcumgr.util.ByteUtil;
import io.runtime.mcumgr.util.CBOR;

public class McuMgrResponseBuilder {
    public static final String TAG = McuMgrResponseBuilder.class.getSimpleName();

    private byte[] mPayload;

    /**
     * Construct a McuMgrResponse.
     * <p>
     * Note: In the case of a CoAP scheme, bytes argument should contain only the CoAP payload,
     * not the entire CoAP packet (i.e. no CoAP header or header options).
     *
     * @param scheme the scheme used by the transporter
     * @param bytes  the mcu manager response. If using a CoAP scheme these bytes should NOT contain
     *               the CoAP header and options.
     * @throws IOException Error parsing response payload into header and return code.
     */
    public McuMgrResponseBuilder(McuMgrScheme scheme, byte[] bytes) throws IOException {
        mPayload = parsePayload(scheme, bytes);
    }

    /**
     * Parse the header from a response.
     *
     * @param scheme the response's scheme
     * @param bytes  the response in bytes (If using a CoAP scheme, this should NOT include the CoAP
     *               header and options).
     * @return the header
     * @throws IOException Error parsing the bytes into an object
     */
    private static McuMgrHeader parseHeader(McuMgrScheme scheme, byte[] bytes)
            throws IOException {
        if (scheme.isCoap()) {
            McuMgrCoapBaseResponse response = CBOR.toObject(bytes, McuMgrCoapBaseResponse.class);
            return McuMgrHeader.fromBytes(response._h);
        } else {
            byte[] header = Arrays.copyOf(bytes, McuMgrHeader.NMGR_HEADER_LEN);
            return McuMgrHeader.fromBytes(header);
        }
    }

    /**
     * Parse the payload from a response.
     *
     * @param scheme the response's scheme
     * @param bytes  the response in bytes (If using a CoAP scheme, this should NOT include the CoAP
     *               header and options).
     * @return the payload
     */
    private static byte[] parsePayload(McuMgrScheme scheme, byte[] bytes) {
        if (scheme.isCoap()) {
            return bytes;
        } else {
            return Arrays.copyOfRange(bytes, McuMgrHeader.NMGR_HEADER_LEN, bytes.length);
        }
    }

    public <T> T build(Class<T> clz) throws IOException {
        Log.v(TAG, "Converting: " + ByteUtil.byteArrayToHex(mPayload, "0x%02X "));
        Log.v(TAG, "Converting response payload of size " + mPayload.length + " to a " + clz.getSimpleName());
        return CBOR.toObject(mPayload, clz);
    }

}
