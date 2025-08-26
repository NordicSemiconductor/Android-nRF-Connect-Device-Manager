/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.viewmodel.mcumgr;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import javax.inject.Inject;
import javax.inject.Named;

import no.nordicsemi.android.mcumgr.McuMgrCallback;
import no.nordicsemi.android.mcumgr.McuMgrTransport;
import no.nordicsemi.android.mcumgr.ble.exception.McuMgrDisconnectedException;
import no.nordicsemi.android.mcumgr.exception.McuMgrException;
import no.nordicsemi.android.mcumgr.managers.DefaultManager;
import no.nordicsemi.android.mcumgr.response.dflt.McuMgrOsResponse;

public class ResetViewModel extends McuMgrViewModel {
    private final DefaultManager manager;

    private final MutableLiveData<McuMgrException> errorLiveData = new MutableLiveData<>();

    @Inject
    ResetViewModel(final DefaultManager manager,
                   @Named("busy") final MutableLiveData<Boolean> state) {
        super(state);
        this.manager = manager;
    }

    @NonNull
    public LiveData<McuMgrException> getError() {
        return errorLiveData;
    }

    public void reset(final @DefaultManager.BootMode int bootMode, final boolean force) {
        setBusy();
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
