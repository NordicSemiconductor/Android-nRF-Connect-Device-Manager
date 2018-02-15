package io.runtime.mcumgr.dfu;

import java.util.Date;

import io.runtime.mcumgr.exception.McuMgrException;

public interface FirmwareUpgradeCallback {
    boolean confirmUpgrade(FirmwareUpgradeManager firmwareUpgrade);

    void onStateChanged(FirmwareUpgradeManager.State prevState, FirmwareUpgradeManager.State newState);

    void onSuccess();

    void onFail(FirmwareUpgradeManager.State state, McuMgrException error);

    void onCancel(FirmwareUpgradeManager.State state);

    void onUploadProgressChanged(int bytesSent, int imageSize, Date ts);
}
