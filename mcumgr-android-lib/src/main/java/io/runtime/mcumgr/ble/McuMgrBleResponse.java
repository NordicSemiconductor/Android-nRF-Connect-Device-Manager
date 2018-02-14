/***************************************************************************
 * Copyright (c) Intellinium SAS, 2014-present
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Intellinium SAS and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Intellinium SAS
 * and its suppliers and may be covered by French and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Intellinium SAS.
 ***************************************************************************/

package io.runtime.mcumgr.ble;

import java.io.IOException;

import io.runtime.mcumgr.McuManager;
import io.runtime.mcumgr.McuMgrResponse;

public class McuMgrBleResponse extends McuMgrResponse {
    public McuMgrBleResponse(byte[] bytes) throws IOException {
        super(McuManager.Scheme.BLE, bytes);
    }

    /* TODO ???? */
    @Override
    public boolean isSuccess() {
        return true;
    }
}
