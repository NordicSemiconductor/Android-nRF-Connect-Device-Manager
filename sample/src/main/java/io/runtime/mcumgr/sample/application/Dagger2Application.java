/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.application;

import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothDevice;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import io.runtime.mcumgr.sample.di.AppInjector;
import io.runtime.mcumgr.sample.di.component.McuMgrSubComponent;
import timber.log.Timber;

public class Dagger2Application extends Application implements HasActivityInjector {

    @Inject
    DispatchingAndroidInjector<Activity> dispatchingAndroidInjector;
    @Inject
    McuMgrSubComponent.Builder mBuilder;

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
    public DispatchingAndroidInjector<Activity> activityInjector() {
        return dispatchingAndroidInjector;
    }

    /**
     * Binds the target {@link BluetoothDevice} with the Dagger2 sub component.
     *
     * @param device the target device.
     */
    public void setTarget(@NonNull final BluetoothDevice device) {
        mBuilder.target(device).build().update(this);
    }
}
