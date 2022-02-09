/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.McuMgrHeader;
import io.runtime.mcumgr.McuMgrScheme;
import io.runtime.mcumgr.exception.McuMgrCoapException;
import io.runtime.mcumgr.response.fs.McuMgrFsUploadResponse;
import io.runtime.mcumgr.response.img.McuMgrImageUploadResponse;
import io.runtime.mcumgr.util.CBOR;

@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
public class McuMgrResponse {

    private final static Logger LOG = LoggerFactory.getLogger(McuMgrResponse.class);

    /**
     * The raw return code found in most McuMgr response payloads. If a rc value is not explicitly
     * stated, a value of 0 is assumed.
     */
    @JsonProperty("rc")
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
     * McuMgr payload for this response. This does not include the McuMgr header for standard
     * schemes and does not include the CoAP header for CoAP schemes.
     */
    private byte[] mPayload;

    /**
     * The CoAP Code used for CoAP schemes, formatted as ((class * 100) + detail).
     */
    private int mCoapCode = 0;

    @JsonCreator
    public McuMgrResponse() {
    }

    /**
     * Return the string representation of the response payload.
     *
     * @return The string representation of the response payload.
     */
    @NotNull
    @Override
    public String toString() {
        try {
            return CBOR.toString(mPayload);
        } catch (IOException e) {
            LOG.error("Failed to parse response", e);
            return "Failed to parse response";
        }
    }

    /**
     * Get the McuMgrHeader for this response.
     *
     * @return The McuMgrHeader.
     */
    @Nullable
    public McuMgrHeader getHeader() {
        return mHeader;
    }

    /**
     * Return the Mcu Manager return code as an int.
     *
     * @return Mcu Manager return code.
     */
    public int getReturnCodeValue() {
        return rc;
    }

    /**
     * Get the return code as an enum.
     *
     * @return The return code enum.
     */
    public McuMgrErrorCode getReturnCode() {
        return McuMgrErrorCode.valueOf(rc);
    }

    /**
     * Returns true if the response payload contains a return code of 0 or no return code. In other
     * words, return true if the command was a success, false otherwise.
     *
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
    public byte @Nullable [] getPayload() {
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
     */
    void initFields(@NotNull McuMgrScheme scheme, byte @NotNull [] bytes,
                    @NotNull McuMgrHeader header, byte @NotNull [] payload) {
        mScheme = scheme;
        mBytes = bytes;
        mHeader = header;
        mPayload = payload;
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
    @NotNull
    public static <T extends McuMgrResponse> T buildResponse(@NotNull McuMgrScheme scheme,
                                                             byte @NotNull [] bytes,
                                                             @NotNull Class<T> type)
            throws IOException {
        if (scheme.isCoap()) {
            throw new IllegalArgumentException("Cannot use this method with a CoAP scheme");
        }

        byte[] payload = Arrays.copyOfRange(bytes, McuMgrHeader.HEADER_LENGTH, bytes.length);
        McuMgrHeader header = McuMgrHeader.fromBytes(Arrays.copyOf(bytes, McuMgrHeader.HEADER_LENGTH));

        // Try decoding response for Image Manager and FS Manager UPLOAD commands really quickly.
        Class<? extends UploadResponse> responseClass = null;
        if (type == McuMgrImageUploadResponse.class
                && bytes[0] == 0x03                     // OP_WRITE_RSP
                && bytes[4] == 0x00 && bytes[5] == 0x01 // GROUP_IMAGE
                && bytes[7] == 0x01) {                  // ID_UPLOAD
           responseClass = McuMgrImageUploadResponse.class;
        } else if (type == McuMgrFsUploadResponse.class
                && bytes[0] == 0x03                     // OP_WRITE_RSP
                && bytes[4] == 0x00 && bytes[5] == 0x08 // GROUP_FS
                && bytes[7] == 0x00) {                  // ID_FILE
            responseClass = McuMgrFsUploadResponse.class;
        }
        if (responseClass != null) {
            try {
                final UploadResponse response = McuMgrResponse.tryDecoding(payload, responseClass);
                if (response != null) {
                    response.initFields(scheme, bytes, header, payload);
                    //noinspection unchecked
                    return (T) response;
                }
            } catch (final Exception e) {
                // Ignore, a CBOR parser will be used below.
            }
        }

        // Initialize response and set fields
        T response = CBOR.toObject(payload, type);
        response.initFields(scheme, bytes, header, payload);

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
    @NotNull
    public static <T extends McuMgrResponse> T buildCoapResponse(@NotNull McuMgrScheme scheme,
                                                                 byte @NotNull [] bytes,
                                                                 byte @NotNull [] header,
                                                                 byte @NotNull [] payload,
                                                                 int codeClass, int codeDetail,
                                                                 Class<T> type)
            throws IOException, McuMgrCoapException {
        // If the code class indicates a CoAP error response, throw a McuMgrCoapException
        if (codeClass == 4 || codeClass == 5) {
            LOG.error("Received CoAP Error response, throwing McuMgrCoapException");
            throw new McuMgrCoapException(bytes, codeClass, codeDetail);
        }

        T response = CBOR.toObject(payload, type);
        response.initFields(scheme, bytes, McuMgrHeader.fromBytes(header), payload);
        int code = (codeClass * 100) + codeDetail;
        response.setCoapCode(code);
        return response;
    }

    /**
     * This method parses the given bytes and reads the LENGTH field from the header.
     * Returns the LENGTH + length of the header. This method may be used to determine whether
     * more bytes must be received before parsing them into a response.
     *
     * @param scheme must be {@link McuMgrScheme#BLE}. COAP schemes are not supported.
     * @param bytes  an array containing the whole or beginning of the message. It must contain
     *               at least the whole header.
     * @return The size of an array containing the header and complete response.
     * @throws IOException                   thrown when the header could not be parsed.
     * @throws UnsupportedOperationException when scheme is not equal to {@link McuMgrScheme#BLE}.
     */
    public static int getExpectedLength(@NotNull McuMgrScheme scheme, byte @NotNull [] bytes)
            throws IOException {
        if (scheme.isCoap()) {
            throw new UnsupportedOperationException("Method not implemented for CoAP");
        } else {
            if (bytes.length < McuMgrHeader.HEADER_LENGTH) {
                throw new IOException("Invalid McuMgrHeader");
            }
            byte[] headerBytes = Arrays.copyOf(bytes, McuMgrHeader.HEADER_LENGTH);
            McuMgrHeader header = McuMgrHeader.fromBytes(headerBytes);
            return header.getLen() + McuMgrHeader.HEADER_LENGTH;
        }
    }

    /**
     * This method tries decoding response for Image Manager UPLOAD command really quickly, skipping
     * using {@link CBOR#toObject(byte[], Class)}, which is very slow. For a message to be properly
     * parsed it can be encoded as map(*) or map(2) with "rc" and "off" parameters in any order.
     *
     * @param payload the CBOR payload as bytes.
     * @return the decoded message, or null.
     */
    private static <T extends UploadResponse> UploadResponse tryDecoding(final byte @NotNull [] payload, Class<T> responseType)
            throws IllegalAccessException, InstantiationException {
        // The response must have "rc" and "off" encoded. The minimum number of bytes is 10.
        if (payload.length < 10)
            return null;

        int offset = 0;

        // The response is encoded as map(*) (0xBF), or map(2) (0xA2).
        int firstByte = payload[offset++] & 0xFF;
        if (firstByte != 0xBF && firstByte != 0xA2)
            return null;

        int rc = -1, off = -1;
        int currentToken = -1; // 0 = "rc", 1 = "off"
        while (offset < payload.length) {
            final int type = (payload[offset] & 0xE0) >> 5;
            final int lowerBits = payload[offset++] & 0x1F;

            switch (type) {
                case 0: // positive int
                    // Values 23 or lower can be read as they are.
                    int value = -1;
                    if (lowerBits <= 23) {
                        value = lowerBits;
                    } else {
                        // This fast method supports only 8, 16 and 32-bit positive integers.
                        switch (lowerBits - 24) {
                            case 0:
                                if (payload.length > offset) {
                                    value = payload[offset] & 0xFF;
                                }
                                offset += 1;
                                break;
                            case 1:
                                if (payload.length > offset + 1) {
                                    value = ((payload[offset] & 0xFF) << 8) | (payload[offset + 1] & 0xFF);
                                }
                                offset += 2;
                                break;
                            case 2:
                                if (payload.length > offset + 3) {
                                    value = ((payload[offset] & 0xFF) << 24) | ((payload[offset + 1] & 0xFF) << 16) | ((payload[offset + 2] & 0xFF) << 8) | (payload[offset + 3] & 0xFF);
                                    if (value < 0) {
                                        return null;
                                    }
                                }
                                offset += 4;
                                break;
                            case 3:
                                // unsupported
                            default:
                                return null;
                        }
                    }
                    if (value >= 0) {
                        if (currentToken == 0)
                            rc = value;
                        else if (currentToken == 1)
                            off = value;
                        currentToken = -1;
                    }
                    break;
                case 3: // string
                    // We are only looking for "rc" and "off", which have lengths 2 and 3.
                    // The below code will return null if a longer token is found.
                    switch (lowerBits) {
                        case 2: // "rc"
                            if (payload.length > offset + 1 && payload[offset] == 0x72 && payload[offset + 1] == 0x63) {
                                currentToken = 0;
                            }
                            break;
                        case 3: // "off"
                            if (payload.length > offset + 2 && payload[offset] == 0x6F && payload[offset + 1] == 0x66 && payload[offset + 2] == 0x66) {
                                currentToken = 1;
                            }
                            break;
                        default:
                            return null;
                    }
                    offset += lowerBits;
                    break;
            }
        }
        final UploadResponse response = responseType.newInstance();
        if (rc >= 0) response.rc = rc;
        if (off >= 0) response.off = off;
        return response;
    }
}

