/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.application;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.util.Log;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;
import io.runtime.mcumgr.sample.di.AppInjector;
import io.runtime.mcumgr.sample.di.component.McuMgrSubComponent;
import no.nordicsemi.android.log.LogContract;
import no.nordicsemi.android.log.timber.nRFLoggerTree;
import timber.log.Timber;

public class Dagger2Application extends Application implements HasAndroidInjector {

    @Inject
    DispatchingAndroidInjector<Object> dispatchingAndroidInjector;
    @Inject
    McuMgrSubComponent.Builder builder;

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
        String deviceName = null;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            deviceName = device.getName();
        }

        // Add a custom logger tree that will log all messages from McuManager to nRF Logger app.
        // The name can be null, it's not an issue.
        final nRFLoggerTree logger = new nRFLoggerTree(this, device.getAddress(), deviceName) {

            // By default, logs are mapped to the corresponding log levels.
            // Here we override the log levels based on the message content, only to make the logs
            // more readable in the nRF Logger app.
            @SuppressLint("WrongConstant")
            @Override
            protected void log(int priority, final String tag, final String message, final Throwable t) {
                if (getSession() == null)
                    return;

                int level = priority;
                // Print messages starting with Sending and Received as APPLICATION level.
                // Those contain parsed CBOR values and are printed in McuMgrBleTransport.
                if (message.startsWith("Sending") || message.startsWith("Received")) {
                    level = LogContract.Log.Level.APPLICATION;
                }
                // RC is returned in case of an error. "err" is returned in Version 2 of SMP protocol.
                if (message.contains("\"rc\":") || message.contains("\"err\":")) {
                    level = Log.WARN;
                }
                super.log(level, tag, message, t);
            }

        };
        Timber.plant(this.logger = logger);

        builder.target(device);
        if (logger.getSession() != null) {
            builder.logSessionUri(logger.getSession().getSessionUri());
        }
        builder.build().update(this);
    }
}
