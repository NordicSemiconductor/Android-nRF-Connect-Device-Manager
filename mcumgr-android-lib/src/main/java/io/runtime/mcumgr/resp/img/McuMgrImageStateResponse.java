/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.runtime.mcumgr.resp.img;

import io.runtime.mcumgr.resp.McuMgrSimpleResponse;

public class McuMgrImageStateResponse extends McuMgrSimpleResponse {
    public ImageSlot[] images;
    public int splitStatus;

    public static class ImageSlot {
        public int slot;
        public String version;
        public byte[] hash;
        public boolean bootable;
        public boolean pending;
        public boolean confirmed;
        public boolean active;
        public boolean permanent;
    }
}
