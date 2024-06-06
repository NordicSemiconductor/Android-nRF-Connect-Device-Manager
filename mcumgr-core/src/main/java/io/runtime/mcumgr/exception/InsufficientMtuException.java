/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.exception;

/**
 * InsufficientMtuExceptions should be thrown by the transporter when the packet from the manager
 * is too large to be sent or (potentially) fragmented. The exception includes the transporter's
 * MTU.
 */
@SuppressWarnings("unused")
public class InsufficientMtuException extends McuMgrException {
    private final int mMtu;
    private final int mDataLength;

    public InsufficientMtuException(int payloadLength, int mtu) {
        super("Payload (" + payloadLength + " bytes) too long for MTU: " + mtu);
        mDataLength = payloadLength;
        mMtu = mtu;
    }

    public int getMtu() {
        return mMtu;
    }

    public int getDataLength() {
        return mDataLength;
    }
}
