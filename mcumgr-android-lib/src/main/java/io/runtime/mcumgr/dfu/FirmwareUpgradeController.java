/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.dfu;

@SuppressWarnings("unused")
public interface FirmwareUpgradeController {

    /**
     * Pause the firmware upgrade.
     */
    void pause();

    /**
     * Resume a paused firmware upgrade.
     */
    void resume();

    /**
     * Cancel the firmware upgrade.
     * The firmware may be cancelled in
     * {@link FirmwareUpgradeManager.State#VALIDATE} or
     * {@link FirmwareUpgradeManager.State#UPLOAD} state.
     * The manager does not try to recover the original firmware after the test or confirm commands
     * have been sent. To undo the upload, confirm the image that have been moved to slot 1 during
     * swap.
     */
    void cancel();

    /**
     * Determine whether the firmware upgrade is paused.
     *
     * @return True if the firmware upgrade is paused, false otherwise.
     */
    boolean isPaused();

    /**
     * Determine whether the firmware upgrade is in progress.
     *
     * @return True if the firmware upgrade is in progress, false otherwise.
     */
    boolean isInProgress();
}
