/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.config;

import io.runtime.mcumgr.response.McuMgrResponse;

public class McuMgrConfigReadResponse extends McuMgrResponse {
    /** The value of the config variable. */
    public String val;
}
