/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.viewmodel.mcumgr;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.inject.Named;

import no.nordicsemi.android.mcumgr.McuMgrCallback;
import no.nordicsemi.android.mcumgr.McuMgrTransport;
import no.nordicsemi.android.mcumgr.ble.exception.McuMgrDisconnectedException;
import no.nordicsemi.android.mcumgr.exception.McuMgrException;
import no.nordicsemi.android.mcumgr.managers.DefaultManager;
import no.nordicsemi.android.mcumgr.managers.SettingsManager;
import no.nordicsemi.android.mcumgr.response.McuMgrResponse;
import no.nordicsemi.android.mcumgr.response.dflt.McuMgrOsResponse;

public class ResetViewModel extends McuMgrViewModel {
    private final DefaultManager manager;
    private final SettingsManager settings;

    private final MutableLiveData<McuMgrException> errorLiveData = new MutableLiveData<>();

    @Inject
    ResetViewModel(final DefaultManager manager,
                   final SettingsManager settings,
                   @Named("busy") final MutableLiveData<Boolean> state) {
        super(state);
        this.manager = manager;
        this.settings = settings;
    }

    @NonNull
    public LiveData<McuMgrException> getError() {
        return errorLiveData;
    }

    public void reset(final @DefaultManager.BootMode int bootMode, final boolean force, final @Nullable String advName) {
        setBusy();
        if (advName != null && !advName.isEmpty()) {
            settings.write("fw_loader/adv_name", advName.getBytes(StandardCharsets.UTF_8), new McuMgrCallback<>() {
                @Override
                public void onResponse(@NotNull McuMgrResponse response) {
                    settings.save(new McuMgrCallback<>() {
                        @Override
                        public void onResponse(@NotNull McuMgrResponse response) {
                            // Finally, with the name set, reset the device.
                            reset(bootMode, force);
                        }

                        @Override
                        public void onError(@NotNull McuMgrException error) {
                            errorLiveData.postValue(error);
                            postReady();
                        }
                    });
                }

                @Override
                public void onError(@NonNull final McuMgrException error) {
                    // Seems like setting the name isn't supported.
                    // Let the user know, don't pretend it worked.
                    errorLiveData.postValue(error);
                    postReady();
                }
            });
        } else {
            reset(bootMode, force);
        }
    }

    private void reset(final @DefaultManager.BootMode int bootMode, final boolean force) {
        manager.reset(bootMode, force, new McuMgrCallback<>() {
            @Override
            public void onResponse(@NonNull final McuMgrOsResponse response) {
                manager.getTransporter().addObserver(new McuMgrTransport.ConnectionObserver() {
                    @Override
                    public void onConnected() {
                        // ignore
                    }

                    @Override
                    public void onDisconnected() {
                        manager.getTransporter().removeObserver(this);
                        postReady();
                    }
                });
            }

            @Override
            public void onError(@NonNull final McuMgrException error) {
                if (!(error instanceof McuMgrDisconnectedException)) {
                    errorLiveData.postValue(error);
                }
                postReady();
            }
        });
    }
}
