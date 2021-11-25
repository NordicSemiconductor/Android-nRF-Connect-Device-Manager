/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.di.module;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import dagger.Module;
import dagger.Provides;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.sample.di.McuMgrScope;
import io.runtime.mcumgr.sample.observable.ObservableMcuMgrBleTransport;

@Module
public class McuMgrTransportModule {

    @Provides
    @McuMgrScope
    @NonNull
    static McuMgrTransport provideMcuMgrTransport(@NonNull final Context context,
                                                  @NonNull final BluetoothDevice device,
                                                  @NonNull final Handler handler) {
        return new ObservableMcuMgrBleTransport(context, device, handler);
    }

    @Provides
    @McuMgrScope
    @NonNull
    static HandlerThread provideTransportHandlerThread() {
        final HandlerThread handlerThread = new HandlerThread("McuMgrTransport");
        handlerThread.start(); // The handler thread is stopped in MainViewModel#onCleard().
        return handlerThread;
    }

    @Provides
    @McuMgrScope
    @NonNull
    static Handler provideTransportHandler(@NonNull final HandlerThread handlerThread) {
        return new Handler(handlerThread.getLooper());
    }
}
