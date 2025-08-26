/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr;


import org.jetbrains.annotations.NotNull;

import no.nordicsemi.android.mcumgr.exception.McuMgrException;
import no.nordicsemi.android.mcumgr.response.McuMgrResponse;

/**
 * Callback for asynchronous Mcu Manager commands.
 */
public interface McuMgrCallback<T extends McuMgrResponse> {
    /**
     * Mcu Manager has received a response.
     *
     * @param response the response.
     */
    void onResponse(@NotNull T response);

    /**
     * Mcu Manager has encountered a transport error while sending the command.
     *
     * @param error the error.
     */
    void onError(@NotNull McuMgrException error);
}
