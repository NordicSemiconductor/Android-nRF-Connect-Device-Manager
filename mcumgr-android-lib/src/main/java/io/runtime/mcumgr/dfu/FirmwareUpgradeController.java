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
