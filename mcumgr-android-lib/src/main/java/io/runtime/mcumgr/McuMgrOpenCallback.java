/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr;

public interface McuMgrOpenCallback {

    void onOpen();

    void onOpenError();
}
