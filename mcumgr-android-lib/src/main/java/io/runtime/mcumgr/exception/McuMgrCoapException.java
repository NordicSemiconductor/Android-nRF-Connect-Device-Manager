package io.runtime.mcumgr.exception;

import io.runtime.mcumgr.McuMgrCallback;

/**
 * A McuMgrException caused by a CoAP response error. For CoAP transport scheme McuMgr requests
 * which result in a CoAP response error, this exception will be thrown (for
 * synchronous requests) or the {@link McuMgrCallback#onError(McuMgrException)} will be called with
 * this exception (for asynchronous requests).
 *
 * This exception holds all the necessary information to determine the error code and reconstruct
 * the CoAP response.
 */
@SuppressWarnings("unused")
public class McuMgrCoapException extends McuMgrException {
    private byte[] mBytes;
    private int mCodeClass;
    private int mCodeDetail;

    public McuMgrCoapException(byte[] bytes, int codeClass, int codeDetail) {
        super("McuManager CoAP request resulted in an error response: "  + codeClass + ".0" + codeDetail);
        mBytes = bytes;
        mCodeClass = codeClass;
        mCodeDetail = codeDetail;
    }

    /**
     * Get the raw bytes of the response which caused this exception.
     *
     * @return The bytes of the response.
     */
    public byte[] getBytes() {
        return mBytes;
    }

    /**
     * Get the response's code class.
     *
     * @return The response code class.
     */
    public int getCodeClass() {
        return mCodeClass;
    }

    /**
     * Get the response's code detail.
     *
     * @return The response code detail.
     */
    public int getCodeDetail() {
        return mCodeDetail;
    }
}
