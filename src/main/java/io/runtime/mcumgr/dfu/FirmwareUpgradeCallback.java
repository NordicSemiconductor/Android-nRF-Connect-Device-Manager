package io.runtime.mcumgr.dfu;

import java.util.Date;

import io.runtime.mcumgr.exception.McuMgrException;

/**
 * Callbacks for firmware upgrades started using the {@link FirmwareUpgradeManager}.
 */
public interface FirmwareUpgradeCallback {

    /**
     * Used to confirm a firmware upgrade before proceeding (the method can be blocked).
     *
     * @param firmwareUpgrade the manager used for the firmware upgrade
     * @return true if the upgrade was confirmed, false otherwise
     */
    boolean confirmUpgrade(FirmwareUpgradeManager firmwareUpgrade);

    /**
     * Called when the firmware upgrade changes state (see {@link FirmwareUpgradeManager.State}).
     * @param prevState previous state
     * @param newState new state
     */
    void onStateChanged(FirmwareUpgradeManager.State prevState,
                        FirmwareUpgradeManager.State newState);

    /**
     * Called when the firmware upgrade has succeeded.
     */
    void onSuccess();

    /**
     * Called when the firmware upgrade has failed.
     * @param state the state the upgrade failed in
     * @param error the error
     */
    void onFail(FirmwareUpgradeManager.State state, McuMgrException error);

    /**
     * Called when the firmware upgrade has been canceled using the
     * {@link FirmwareUpgradeManager#cancel()} method.
     * @param state the state the upgrade was cancelled in
     */
    void onCancel(FirmwareUpgradeManager.State state);

    /**
     * Called when the {@link FirmwareUpgradeManager.State#UPLOAD} state progress has changed.
     * @param bytesSent the number of bytes sent so far
     * @param imageSize the total number of bytes to send
     * @param ts the time that the successful response packet for the progress was received
     */
    void onUploadProgressChanged(int bytesSent, int imageSize, Date ts);
}
