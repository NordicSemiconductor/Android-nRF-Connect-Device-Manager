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
import no.nordicsemi.android.mcumgr.exception.McuMgrException;
import no.nordicsemi.android.mcumgr.managers.BasicManager;
import no.nordicsemi.android.mcumgr.response.zephyr.basic.McuMgrZephyrBasicResponse;

public class ImageSettingsViewModel extends McuMgrViewModel {
    private final BasicManager manager;

    private final MutableLiveData<McuMgrException> errorLiveData = new MutableLiveData<>();

    @Inject
	ImageSettingsViewModel(final BasicManager manager,
						   @Named("busy") final MutableLiveData<Boolean> state) {
        super(state);
        this.manager = manager;
    }

    @NonNull
    public LiveData<McuMgrException> getError() {
        return errorLiveData;
    }

    public void eraseSettings() {
        setBusy();
        errorLiveData.setValue(null);
        manager.eraseStorage(new McuMgrCallback<>() {
            @Override
            public void onResponse(@NonNull final McuMgrZephyrBasicResponse response) {
                errorLiveData.postValue(null);
                postReady();
            }

            @Override
            public void onError(@NonNull final McuMgrException error) {
                errorLiveData.postValue(error);
                postReady();
            }
        });
    }
}
