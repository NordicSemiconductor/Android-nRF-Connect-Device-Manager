/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.exception;

import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;

/**
 * InsufficientMtuExceptions should be thrown by the transporter when the packet from the manager
 * is too large to be sent or (potentially) fragmented. The exception includes the transporter's
 * MTU. This is used in practice by the {@link FirmwareUpgradeManager} to resize it's packets to
 * fit within the transporter's MTU.
 */
public class InsufficientMtuException extends McuMgrException {
    private int mMtu;
    public InsufficientMtuException(int mtu) {
        super("Insufficient MTU!");
        mMtu = mtu;
    }

    public int getMtu() {
        return mMtu;
    }
}
