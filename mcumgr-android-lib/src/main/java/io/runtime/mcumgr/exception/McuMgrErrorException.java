package io.runtime.mcumgr.exception;

import io.runtime.mcumgr.McuManager;

public class McuMgrErrorException extends McuMgrException {
    private McuManager.Code mCode;
    public McuMgrErrorException(McuManager.Code code) {
        mCode = code;
    }

    @Override
    public String toString() {
        return "McuMgrErrorException: " + mCode.toString() + " (" + mCode.value() + ")";
    }
}
