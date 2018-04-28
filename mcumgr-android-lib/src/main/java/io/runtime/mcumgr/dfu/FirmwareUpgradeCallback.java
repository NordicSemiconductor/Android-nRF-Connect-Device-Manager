/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.runtime.mcumgr.dfu;

import java.util.Date;

import io.runtime.mcumgr.exception.McuMgrException;

/**
 * Callbacks for firmware upgrades started using the {@link FirmwareUpgradeManager}.
 */
public interface FirmwareUpgradeCallback {

    /**
     * Called when the {@link FirmwareUpgradeManager} has started.
     * <p>
     * This callback is used to pass the upgrade manager which can pause/resume/cancel an upgrade
     * to a controller which may not have access to the original object.
     * @param manager the manager
     */
    void onStart(FirmwareUpgradeManager manager);

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
