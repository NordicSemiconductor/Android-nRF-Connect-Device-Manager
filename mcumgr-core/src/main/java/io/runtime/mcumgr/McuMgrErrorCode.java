/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr;


import org.jetbrains.annotations.NotNull;

/**
 * Almost all requests are responded to with an Mcu Manager return code in the response payload.
 * This value determines whether the request has been successful (rc = 0) or has failed (rc != 0).
 * <p>
 * This return code is not supposed to be very descriptive and the actual error reason must be
 * determined based on the request and error code. Since McuManager errors are vague and often the
 * same error code could be caused by different reasons, the best way to debug errors here is
 * step through the handler on the device to determine the cause.
 * <p>
 * List of possible error codes may be found
 * <a href="https://github.com/nrfconnect/sdk-zephyr/blob/main/include/zephyr/mgmt/mcumgr/mgmt/mgmt.h">here</a>.
 */
public enum McuMgrErrorCode {
    /**
     * Success.
     */
    OK(0),
    /**
     * Unknown error.
     */
    UNKNOWN(1),
    /**
     * The device has encountered an error due to running out of memory.
     */
    NO_MEMORY(2),
    /**
     * The request header/payload is malformed or payload values are incorrect.
     */
    IN_VALUE(3),
    /**
     * Timeout error.
     */
    TIMEOUT(4),
    /**
     * No entry was found for the request. This commonly means that the command group has not been
     * enabled on the device, although the exact meaning.
     */
    NO_ENTRY(5),
    /**
     * The device is not currently in a state to handle the request.
     */
    BAD_STATE(6),
    /**
     * The response is too large.
     */
    TOO_LARGE(7),
    /**
     * Command is not supported.
     */
    NOT_SUPPORTED(8),
    /**
     * Corrupt.
     */
    CORRUPT(9),
    /**
     * Command blocked by processing of other command.
     */
    BUSY(10),
    /**
     * Access to specific function, command or resource denied.
     */
    ACCESS_DENIED(11),
    /**
     * User errors defined from 256 onwards.
     */
    PER_USER(256);

    private final int mCode;

    McuMgrErrorCode(int code) {
        mCode = code;
    }

    public int value() {
        return mCode;
    }

    @NotNull
    @Override
    public String toString() {
        return super.toString() + " (" + mCode + ")";
    }

    @NotNull
    public static McuMgrErrorCode valueOf(int error) {
        switch (error) {
            case 0: {
                return OK;
            }
            // case 1 is equal to default case
            case 2: {
                return NO_MEMORY;
            }
            case 3: {
                return IN_VALUE;
            }
            case 4: {
                return TIMEOUT;
            }
            case 5: {
                return NO_ENTRY;
            }
            case 6: {
                return BAD_STATE;
            }
            case 7: {
                return TOO_LARGE;
            }
            case 8: {
                return NOT_SUPPORTED;
            }
            case 9: {
                return CORRUPT;
            }
            case 10: {
                return BUSY;
            }
            case 11: {
                return ACCESS_DENIED;
            }
            case 256: {
                return PER_USER;
            }
            default: {
                return UNKNOWN;
            }
        }
    }
}
