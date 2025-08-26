/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.di.module;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;

import dagger.Module;
import dagger.Provides;
import no.nordicsemi.android.mcumgr.McuMgrTransport;
import no.nordicsemi.android.mcumgr.sample.di.McuMgrScope;
import no.nordicsemi.android.mcumgr.sample.observable.ObservableMcuMgrBleTransport;

@Module
public class McuMgrTransportModule {

    @Provides
    @McuMgrScope
    @NonNull
    static McuMgrTransport provideMcuMgrTransport(@NonNull final Context context,
                                                  @NonNull final BluetoothDevice device,
                                                  @NonNull final HandlerThread handlerThread) {
        final Handler handler = new Handler(handlerThread.getLooper());
        final ObservableMcuMgrBleTransport transport = new ObservableMcuMgrBleTransport(context, device, handler);
        transport.setOnReleasedCallback(handlerThread::quitSafely);
        return transport;
    }

    @Provides
    @McuMgrScope
    @NonNull
    static HandlerThread provideTransportHandlerThread() {
        final HandlerThread handlerThread = new HandlerThread("McuMgrTransport");
        handlerThread.start(); // The handler thread is stopped in MainViewModel#onCleard().
        return handlerThread;
    }
}
