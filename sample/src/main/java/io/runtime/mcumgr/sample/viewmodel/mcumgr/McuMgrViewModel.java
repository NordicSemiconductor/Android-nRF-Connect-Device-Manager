/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.viewmodel.mcumgr;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Named;

public class McuMgrViewModel extends ViewModel {
	private final MutableLiveData<Boolean> mBusyStateLiveData;

	@Inject
	McuMgrViewModel(@Named("busy") final MutableLiveData<Boolean> state) {
		mBusyStateLiveData = state;
	}

	@NonNull
	public LiveData<Boolean> getBusyState() {
		return mBusyStateLiveData;
	}

	void setBusy() {
		mBusyStateLiveData.setValue(true);
	}

	void postBusy() {
		mBusyStateLiveData.postValue(true);
	}

	void setReady() {
		mBusyStateLiveData.setValue(false);
	}

	void postReady() {
		mBusyStateLiveData.postValue(false);
	}
}
