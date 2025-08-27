/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.application;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationManagerCompat;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;
import no.nordicsemi.android.log.LogContract;
import no.nordicsemi.android.log.timber.nRFLoggerTree;
import no.nordicsemi.android.mcumgr.sample.di.AppInjector;
import no.nordicsemi.android.mcumgr.sample.di.component.McuMgrSubComponent;
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
        // Hack based on: https://github.com/JakeWharton/timber/issues/484#issuecomment-2008724303
        //noinspection DataFlowIssue
        Timber.plant((Timber.Tree) (Object) new Timber.DebugTree());

        // Register the notification channel for Fast Pair firmware updates.
        registerNotificationChannels();
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
            protected void log(int priority, final String tag, final @NonNull String message, final Throwable t) {
                if (getSession() == null)
                    return;

                int level = priority;
                // Print messages starting with Sending and Received as APPLICATION level.
                // Those contain parsed CBOR values and are printed in McuMgrBleTransport.
                if (message.startsWith("Sending") || message.startsWith("Received") || message.startsWith("Upload completed")) {
                    level = LogContract.Log.Level.APPLICATION;
                }
                // RC is returned in case of an error. "err" is returned in Version 2 of SMP protocol.
                if (message.contains("\"rc\":") || message.contains("\"err\":")) {
                    level = Log.WARN;
                }
                // Skip the tag, Proguard obfuscates it anyway.
                super.log(level, null, message, t);
            }

        };
        Timber.plant(this.logger = logger);

        builder.target(device);
        if (logger.getSession() != null) {
            builder.logSessionUri(logger.getSession().getSessionUri());
        }
        builder.build().update(this);
    }

    /**
     * Registers Notifications Channels.
     */
    private void registerNotificationChannels() {
        // https://developers.google.com/nearby/fast-pair/companion-apps#firmware_update_intent
        final NotificationChannelCompat channel = new NotificationChannelCompat
                .Builder(FastPairFirmwareUpdateReceiver.NOTIFICATION_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName("Fast Pair")
                .setDescription("Notifications regarding Fast Pair firmware updates. These notifications will only show up when you have a Fast Pair accessory with the companion app set to nRF Connect Device Manager.")
                .setLightsEnabled(true)
                .setLightColor(0x0000FF)
                .setShowBadge(false)
                .build();

        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.createNotificationChannel(channel);
    }
}
