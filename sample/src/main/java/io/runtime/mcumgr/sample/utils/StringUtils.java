/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.ble.exception.McuMgrBluetoothDisabledException;
import io.runtime.mcumgr.ble.exception.McuMgrDisconnectedException;
import io.runtime.mcumgr.ble.exception.McuMgrNotSupportedException;
import io.runtime.mcumgr.ble.exception.McuMgrUnsupportedConfigurationException;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.exception.McuMgrTimeoutException;
import io.runtime.mcumgr.response.HasReturnCode;
import io.runtime.mcumgr.sample.R;

public class StringUtils {
    private final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String toHex(final byte[] data) {
        if (data == null || data.length == 0)
            return "";

        final char[] out = new char[data.length * 2];
        for (int j = 0; j < data.length; j++) {
            int v = data[j] & 0xFF;
            out[j * 2] = HEX_ARRAY[v >>> 4];
            out[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(out);
    }

    @Nullable
    public static String toString(@NonNull final Context context, @Nullable final McuMgrException error) {
        if (error instanceof McuMgrErrorException e) {
            final HasReturnCode.GroupReturnCode groupCode = e.getGroupCode();
            if (groupCode != null) {
                int arrayId = switch (groupCode.group) {
                    case 0 -> R.array.mcu_mgr_os_error;
                    case 1 -> R.array.mcu_mgr_img_error;
                    case 3 -> R.array.mcu_mgr_settings_error;
                    case 8 -> R.array.mcu_mgr_fs_error;
                    case 9 -> R.array.mcu_mgr_shell_error;
                    case 63 -> R.array.mcu_mgr_zephyr_basic_error;
                    default -> -1;
                };
                if (arrayId != -1) {
                    final String[] errors = context.getResources().getStringArray(arrayId);
                    if (groupCode.rc < errors.length)
                        return errors[groupCode.rc];
                }
                return context.getString(R.string.mcu_mgr_group_error_other, groupCode.rc, groupCode.group);
            }
            final McuMgrErrorCode code = e.getCode();
            final String[] errors = context.getResources().getStringArray(R.array.mcu_mgr_error);
            if (code.value() < errors.length)
                return errors[code.value()];
            return context.getString(R.string.mcu_mgr_error_other, code.value());
        } else if (error instanceof McuMgrDisconnectedException ||
                   error instanceof McuMgrBluetoothDisabledException) {
            return context.getString(R.string.status_disconnected);
        } else if (error instanceof McuMgrNotSupportedException) {
            return context.getString(R.string.status_not_supported);
        } else if (error instanceof McuMgrUnsupportedConfigurationException) {
            return context.getString(R.string.status_unsupported_configuration);
        } else if (error instanceof McuMgrTimeoutException) {
            return context.getString(R.string.status_connection_timeout);
        }
        return error != null ?
                error.getCause() != null ?
                        error.getCause().getMessage() :
                        error.getMessage() :
                null;
    }
}
