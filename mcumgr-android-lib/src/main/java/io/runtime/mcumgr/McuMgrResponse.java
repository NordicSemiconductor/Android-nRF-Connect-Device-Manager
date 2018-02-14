package io.runtime.mcumgr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.IOException;
import java.util.Arrays;

import io.runtime.mcumgr.util.CBOR;

public abstract class McuMgrResponse {

	private McuManager.Scheme mScheme;
	private byte[] mBytes;
	private McuMgrHeader mHeader;
	private int mRc = -1;
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
	 * @return true if a Mcu Manager response was received successfully (i.e. no CoAP error), false
	 * otherwise.
	 */
	public abstract boolean isSuccess();

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
		return mRc;
	}

	/**
	 * Get the return code as an enum
	 *
	 * @return the return code enum
	 */
	public McuManager.Code getRc() {
		return McuManager.Code.valueOf(mRc);
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
	 *
	 * @return the scheme
	 */
	public McuManager.Scheme getScheme() {
		return mScheme;
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
	public static McuMgrHeader parseHeader(McuManager.Scheme scheme, byte[] bytes)
			throws IOException {
		if (scheme.isCoap()) {
			CoapBaseResponse response = CBOR.toObject(bytes, CoapBaseResponse.class);
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
	public static byte[] parsePayload(McuManager.Scheme scheme, byte[] bytes) {
		if (scheme.isCoap()) {
			return bytes;
		} else {
			return Arrays.copyOfRange(bytes, McuMgrHeader.NMGR_HEADER_LEN, bytes.length);
		}
	}

	/**
	 * Parse the return code from a response payload.
	 *
	 * @param payload the response payload
	 * @return the return code
	 * @throws IOException Error parsing the return code from the payload
	 */
	public static int parseRc(byte[] payload) throws IOException {
		return CBOR.toObject(payload, BaseResponse.class).rc;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class BaseResponse {
		public int rc;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CoapBaseResponse {
		public byte[] _h;
		public int rc;
	}
}
