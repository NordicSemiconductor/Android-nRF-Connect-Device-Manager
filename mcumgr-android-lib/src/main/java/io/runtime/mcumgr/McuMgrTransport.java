/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr;

import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.resp.McuMgrResponse;

public abstract class McuMgrTransport {
    private McuMgrScheme mScheme;

    protected McuMgrTransport(McuMgrScheme scheme) {
        mScheme = scheme;
    }

    McuMgrScheme getScheme() {
        return mScheme;
    }

    public abstract void open(McuMgrOpenCallback cb);

    public abstract void close();

    public abstract <T extends McuMgrResponse> T send(byte[] payload, Class<T> responseType) throws McuMgrException;

    public abstract <T extends McuMgrResponse> void send(byte[] payload, Class<T> responseType,
                                                         McuMgrCallback<T> callback);

    /**
     * Some transporter may need to initialize the communication with the GATT server again before
     * continuing.
     *
     * @return true to reinitialize, false otherwise
     */
    public boolean initAfterReset() {
        return false;
    }
}
