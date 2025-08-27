/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.viewmodel.mcumgr;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import javax.inject.Inject;
import javax.inject.Named;

public class McuMgrViewModel extends ViewModel {
    private final MutableLiveData<Boolean> busyStateLiveData;

    @Inject
    McuMgrViewModel(@Named("busy") final MutableLiveData<Boolean> state) {
        busyStateLiveData = state;
    }

    @NonNull
    public LiveData<Boolean> getBusyState() {
        return busyStateLiveData;
    }

    void setBusy() {
        busyStateLiveData.setValue(true);
    }

    void postBusy() {
        busyStateLiveData.postValue(true);
    }

    void setReady() {
        busyStateLiveData.setValue(false);
    }

    void postReady() {
        busyStateLiveData.postValue(false);
    }
}
