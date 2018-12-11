/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.exception;

import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;

/**
 * Used to convey errors caused by an {@link McuMgrErrorCode} within a response. This is used in
 * practice by {@link FirmwareUpgradeManager} to send a failure callback with the
 * {@link McuMgrErrorCode} that caused the failure.
 */
@SuppressWarnings("unused")
public class McuMgrTimeoutException extends McuMgrException {
}
