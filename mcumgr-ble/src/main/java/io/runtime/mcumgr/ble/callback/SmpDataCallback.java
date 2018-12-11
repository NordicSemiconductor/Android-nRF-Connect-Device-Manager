/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.ble.callback;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;

import io.runtime.mcumgr.McuMgrScheme;
import io.runtime.mcumgr.response.McuMgrResponse;
import no.nordicsemi.android.ble.callback.profile.ProfileDataCallback;
import no.nordicsemi.android.ble.data.Data;

public abstract class SmpDataCallback<T extends McuMgrResponse>
        implements ProfileDataCallback, SmpCallback<T> {
    private final Class<T> responseType;

    protected SmpDataCallback(@NonNull Class<T> responseType) {
        this.responseType = responseType;
    }

    @Override
    public void onDataReceived(@NonNull BluetoothDevice device, @NonNull Data data) {
        try {
            T response = McuMgrResponse.buildResponse(McuMgrScheme.BLE, data.getValue(), responseType);
            onResponseReceived(device, response);
        } catch (final Exception e) {
            onInvalidDataReceived(device, data);
        }
    }
}
