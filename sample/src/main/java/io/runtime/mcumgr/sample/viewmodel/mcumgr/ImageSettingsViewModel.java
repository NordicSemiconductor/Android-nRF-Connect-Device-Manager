/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.viewmodel.mcumgr;

import javax.inject.Inject;
import javax.inject.Named;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.managers.BasicManager;
import io.runtime.mcumgr.response.McuMgrResponse;

public class ImageSettingsViewModel extends McuMgrViewModel {
    private final BasicManager mManager;

    private final MutableLiveData<McuMgrException> mErrorLiveData = new MutableLiveData<>();

    @Inject
	ImageSettingsViewModel(final BasicManager manager,
						   @Named("busy") final MutableLiveData<Boolean> state) {
        super(state);
        mManager = manager;
    }

    @NonNull
    public LiveData<McuMgrException> getError() {
        return mErrorLiveData;
    }

    public void eraseSettings() {
        setBusy();
        mErrorLiveData.setValue(null);
        mManager.eraseStorage(new McuMgrCallback<McuMgrResponse>() {
            @Override
            public void onResponse(@NonNull final McuMgrResponse response) {
                mErrorLiveData.postValue(null);
                postReady();
            }

            @Override
            public void onError(@NonNull final McuMgrException error) {
                mErrorLiveData.postValue(error);
                postReady();
            }
        });
    }
}
