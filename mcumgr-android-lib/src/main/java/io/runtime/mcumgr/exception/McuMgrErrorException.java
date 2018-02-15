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
/* TODO: add runtime copyright */

package io.runtime.mcumgr.exception;

import io.runtime.mcumgr.McuMgrErrorCode;

public class McuMgrErrorException extends McuMgrException {
    private McuMgrErrorCode mCode;

    public McuMgrErrorException(McuMgrErrorCode code) {
        mCode = code;
    }

    @Override
    public String toString() {
        return "McuMgrErrorException: " + mCode.toString() + " (" + mCode.value() + ")";
    }
}
