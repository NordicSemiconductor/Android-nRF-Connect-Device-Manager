/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.dfu;

import io.runtime.mcumgr.exception.McuMgrException;

/**
 * Callbacks for firmware upgrades started using the {@link FirmwareUpgradeManager}.
 */
public interface FirmwareUpgradeCallback {

    /**
     * Called when the {@link FirmwareUpgradeManager} has started.
     * <p>
     * This callback is used to pass the upgrade controller which can pause/resume/cancel
     * an upgrade to a controller which may not have access to the original object.
     *
     * @param controller the upgrade controller.
     */
    void onUpgradeStarted(FirmwareUpgradeController controller);

    /**
     * Called when the firmware upgrade changes state.
     *
     * @param prevState previous state.
     * @param newState  new state.
     * @see FirmwareUpgradeManager.State
     */
    void onStateChanged(FirmwareUpgradeManager.State prevState,
                        FirmwareUpgradeManager.State newState);

    /**
     * Called when the firmware upgrade has succeeded.
     */
    void onUpgradeCompleted();

    /**
     * Called when the firmware upgrade has failed.
     *
     * @param state the state the upgrade failed in.
     * @param error the error.
     */
    void onUpgradeFailed(FirmwareUpgradeManager.State state, McuMgrException error);

    /**
     * Called when the firmware upgrade has been canceled using the
     * {@link FirmwareUpgradeManager#cancel()} method. The upgrade may be cancelled only during
     * uploading the image. When the image is uploaded, the test and/or confirm commands will be
     * sent depending on the {@link FirmwareUpgradeManager#setMode(FirmwareUpgradeManager.Mode)}.
     *
     * @param state the state the upgrade was cancelled in.
     */
    void onUpgradeCanceled(FirmwareUpgradeManager.State state);

    /**
     * Called when the {@link FirmwareUpgradeManager.State#UPLOAD} state progress has changed.
     *
     * @param bytesSent the number of bytes sent so far.
     * @param imageSize the total number of bytes to send.
     * @param timestamp the time that the successful response packet for the progress was received.
     */
    void onUploadProgressChanged(int bytesSent, int imageSize, long timestamp);
}
