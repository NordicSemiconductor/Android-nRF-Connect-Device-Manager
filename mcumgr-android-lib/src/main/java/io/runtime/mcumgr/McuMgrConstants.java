/*
 *  Copyright (c) Intellinium SAS, 2014-present
 *  All Rights Reserved.
 *
 *  NOTICE:  All information contained herein is, and remains
 *  the property of Intellinium SAS and its suppliers,
 *  if any.  The intellectual and technical concepts contained
 *  herein are proprietary to Intellinium SAS
 *  and its suppliers and may be covered by French and Foreign Patents,
 *  patents in process, and are protected by trade secret or copyright law.
 *  Dissemination of this information or reproduction of this material
 *  is strictly forbidden unless prior written permission is obtained
 *  from Intellinium SAS.
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
