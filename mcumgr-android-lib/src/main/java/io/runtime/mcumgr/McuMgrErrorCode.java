/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr;

/**
 * Almost all requests are responded to with an Mcu Manager return code in the response payload.
 * This value determines whether the request has been successful (rc = 0) or has failed (rc != 0).
 *
 * This return code is not supposed to be very descriptive and the actual error reason must be
 * determined based on the request and error code.
 */
public enum McuMgrErrorCode {
    OK(0),
    UNKNOWN(1),
    NO_MEMORY(2),
    IN_VALUE(3),
    TIMEOUT(4),
    NO_ENTRY(5),
    BAD_STATE(6),
    TOO_LARGE(7),
    NOT_SUP(8),
    PERM_ERROR(256);

    private int mCode;

    McuMgrErrorCode(int code) {
        mCode = code;
    }

    public int value() {
        return mCode;
    }

    @Override
    public String toString() {
        return "McuMgrError: " + super.toString() + "(" + mCode + ")";
    }

    public static McuMgrErrorCode valueOf(int error) {
        switch (error) {
            case 0:
                return OK;
            case 1:
                return UNKNOWN;
            case 2:
                return NO_MEMORY;
            case 3:
                return IN_VALUE;
            case 4:
                return TIMEOUT;
            case 5:
                return NO_ENTRY;
            case 6:
                return BAD_STATE;
            case 7:
                return TOO_LARGE;
            case 8:
                return NOT_SUP;
            case 256:
                return PERM_ERROR;
            default:
                return null;
        }
    }
}
