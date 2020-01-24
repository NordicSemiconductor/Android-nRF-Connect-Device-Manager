/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.ble.callback;

import android.bluetooth.BluetoothDevice;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.runtime.mcumgr.response.McuMgrResponse;
import no.nordicsemi.android.ble.data.Data;

public final class SmpResponse<T extends McuMgrResponse> extends SmpDataCallback<T> {
    @Nullable
    private T response;
    private boolean valid;
    private Data data;

    public SmpResponse(Class<T> responseType) {
        super(responseType);
    }

    @Override
    public void onDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {
        this.data = data;
        super.onDataReceived(device, data);
    }

    @Override
    public void onResponseReceived(@NonNull BluetoothDevice device, @NonNull T response) {
        this.response = response;
        this.valid = true;
    }

    @Override
    public void onInvalidDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {
        this.valid = false;
    }

    @Nullable
    public T getResponse() {
        return response;
    }

    @NonNull
    public Data getRawData() {
        return data;
    }

    public boolean isValid() {
        return valid;
    }
}
