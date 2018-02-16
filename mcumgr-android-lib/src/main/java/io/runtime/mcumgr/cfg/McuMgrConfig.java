/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.runtime.mcumgr.cfg;

import android.util.Log;

public class McuMgrConfig {
    private static final String TAG = McuMgrConfig.class.getSimpleName();

    public static int IMG_HEADER_MAGIC;
    public static int IMAGE_TLV_SHA256;
    public static int IMAGE_HASH_LEN;
    public static int TLV_INFO_MAGIC;
    public static boolean HAS_TLV_INFO;

    static {
        defaultConfig();
    }

    private McuMgrConfig() {
    }

    public static void defaultConfig() {
        IMG_HEADER_MAGIC = 0x96f3b83d;
        IMAGE_TLV_SHA256 = 0x10;
        IMAGE_HASH_LEN = 32;
        TLV_INFO_MAGIC = 0x6907;
        HAS_TLV_INFO = true;
    }

    public static void legacy() {
        Log.w(TAG, "You are using an old version of mynewt. Please upgrade it, as future changes may not be " +
                "integrated in this library!");

        IMG_HEADER_MAGIC = 0x96f3b83c;
        IMAGE_TLV_SHA256 = 0x01;
        IMAGE_HASH_LEN = 32;
        TLV_INFO_MAGIC = 0x6907;
        HAS_TLV_INFO = false;
    }
}
