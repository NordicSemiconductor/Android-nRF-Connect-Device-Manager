/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.img;

import io.runtime.mcumgr.response.McuMgrResponse;

public class McuMgrImageUploadResponse extends McuMgrResponse {
    /** The offset. Number of bytes that were received. */
    public int off;
}
