package io.runtime.mcumgr;

import io.runtime.mcumgr.exception.McuMgrException;

public abstract class McuMgrTransport {
    private McuManager.Scheme mScheme;
    protected McuMgrTransport(McuManager.Scheme scheme) {
        mScheme = scheme;
    }
    public McuManager.Scheme getScheme() {
        return mScheme;
    }
    public abstract McuMgrResponse send(byte[] payload) throws McuMgrException;
    public abstract void send(byte[] payload, McuMgrCallback callback);
}
