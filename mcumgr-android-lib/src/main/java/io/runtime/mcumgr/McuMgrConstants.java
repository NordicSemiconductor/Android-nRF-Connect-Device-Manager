/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr;

public class McuMgrConstants {
    public final static String COAP_URI = "/omgr";
    public final static String HEADER_KEY = "_h";

    // Newt Manager operation codes
    public final static int OP_READ = 0;
    public final static int OP_READ_RSP = 1;
    public final static int OP_WRITE = 2;
    public final static int OP_WRITE_RSP = 3;

    // Newt Manager groups
    public final static int GROUP_DEFAULT = 0;
    public final static int GROUP_IMAGE = 1;
    public final static int GROUP_STATS = 2;
    public final static int GROUP_CONFIG = 3;
    public final static int GROUP_LOGS = 4;
    public final static int GROUP_CRASH = 5;
    public final static int GROUP_PERUSER = 64;
}
