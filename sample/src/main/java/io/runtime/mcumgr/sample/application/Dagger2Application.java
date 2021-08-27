/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.application;

import android.app.Application;
import android.bluetooth.BluetoothDevice;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;
import io.runtime.mcumgr.sample.di.AppInjector;
import io.runtime.mcumgr.sample.di.component.McuMgrSubComponent;
import no.nordicsemi.android.log.timber.nRFLoggerTree;
import timber.log.Timber;

public class Dagger2Application extends Application implements HasAndroidInjector {

    @Inject
    DispatchingAndroidInjector<Object> dispatchingAndroidInjector;
    @Inject
    McuMgrSubComponent.Builder mBuilder;

    private Timber.Tree logger;

    @Override
    public void onCreate() {
        super.onCreate();

        // The app injector makes sure that all activities and fragments that implement Injectable
        // are injected in onCreate(...) or onActivityCreated(...)
        AppInjector.init(this);

        // Plant a Timber DebugTree to collect logs from sample app and McuManager
        Timber.plant(new Timber.DebugTree());
    }

    @Override
    public AndroidInjector<Object> androidInjector() {
        return dispatchingAndroidInjector;
    }

    /**
     * Binds the target {@link BluetoothDevice} with the Dagger2 sub component.
     *
     * @param device the target device.
     */
    public void setTarget(@NonNull final BluetoothDevice device) {
        if (logger != null) {
            Timber.uproot(logger);
            logger = null;
        }
        Timber.plant(logger = new nRFLoggerTree(this, "Device Manager", device.getName()));
        mBuilder.target(device).build().update(this);
    }
}
