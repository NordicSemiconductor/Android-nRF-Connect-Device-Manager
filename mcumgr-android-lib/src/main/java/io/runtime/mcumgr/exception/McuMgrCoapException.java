package io.runtime.mcumgr.exception;

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

    public byte[] getBytes() {
        return mBytes;
    }
    public int getCodeClass() {
        return mCodeClass;
    }
    public int getCodeDetail() {
        return mCodeDetail;
    }
}
