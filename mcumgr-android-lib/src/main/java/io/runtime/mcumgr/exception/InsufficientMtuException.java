/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.exception;

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
