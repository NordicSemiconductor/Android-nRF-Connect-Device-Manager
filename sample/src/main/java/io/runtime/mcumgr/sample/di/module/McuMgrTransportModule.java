/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.di.module;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import javax.inject.Named;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import dagger.Module;
import dagger.Provides;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.ble.McuMgrBleTransport;
import io.runtime.mcumgr.sample.di.McuMgrScope;

@Module
public class McuMgrTransportModule {

    @Provides
    @Named("busy")
    @McuMgrScope
    static MutableLiveData<Boolean> provideBusyStateLiveData() {
        return new MutableLiveData<>();
    }

    @Provides
    @McuMgrScope
    static McuMgrTransport provideMcuMgrTransport(@NonNull final Context context,
                                                  @NonNull final BluetoothDevice device) {
        return new McuMgrBleTransport(context, device);
    }
}
