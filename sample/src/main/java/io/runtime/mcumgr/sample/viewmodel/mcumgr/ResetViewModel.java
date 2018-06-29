/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.viewmodel.mcumgr;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Named;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.managers.DefaultManager;
import io.runtime.mcumgr.response.McuMgrResponse;

public class ResetViewModel extends McuMgrViewModel {
	private final DefaultManager mManager;

	private final MutableLiveData<String> mErrorLiveData = new MutableLiveData<>();

	@Inject
	ResetViewModel(final DefaultManager manager,
				   @Named("busy") final MutableLiveData<Boolean> state) {
		super(state);
		mManager = manager;
	}

	@NonNull
	public LiveData<String> getError() {
		return mErrorLiveData;
	}

	public void reset() {
		setBusy();
		mManager.reset(new McuMgrCallback<McuMgrResponse>() {
			@Override
			public void onResponse(@NonNull final McuMgrResponse response) {
				mManager.getTransporter().addObserver(new McuMgrTransport.ConnectionObserver() {
					@Override
					public void onConnected() {
						// ignore
					}

					@Override
					public void onDisconnected() {
						mManager.getTransporter().removeObserver(this);
						postReady();
					}
				});
			}

			@Override
			public void onError(@NonNull final McuMgrException error) {
				mErrorLiveData.postValue(error.getMessage());
				postReady();
			}
		});
	}
}
