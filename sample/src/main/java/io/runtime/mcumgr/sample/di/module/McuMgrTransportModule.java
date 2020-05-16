/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.di.module;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.sample.di.McuMgrScope;
import io.runtime.mcumgr.sample.observable.ObservableMcuMgrBleTransport;

@Module
public class McuMgrTransportModule {

    @Provides
    @Named("busy")
    @McuMgrScope
    @NonNull
    static MutableLiveData<Boolean> provideBusyStateLiveData() {
        return new MutableLiveData<>();
    }

    @Provides
    @McuMgrScope
    @NonNull
    static McuMgrTransport provideMcuMgrTransport(@NonNull final Context context,
                                                  @NonNull final BluetoothDevice device) {
        return new ObservableMcuMgrBleTransport(context, device);
    }
}
