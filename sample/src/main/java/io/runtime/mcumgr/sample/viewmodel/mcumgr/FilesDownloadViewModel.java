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

import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.ble.McuMgrBleTransport;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.managers.FsManager;
import io.runtime.mcumgr.sample.viewmodel.SingleLiveEvent;
import no.nordicsemi.android.ble.ConnectionPriorityRequest;

@SuppressWarnings("unused")
public class FilesDownloadViewModel extends McuMgrViewModel
		implements FsManager.FileDownloadCallback {
	private final FsManager mManager;

	private final MutableLiveData<Integer> mProgressLiveData = new MutableLiveData<>();
	private final MutableLiveData<byte[]> mResponseLiveData = new MutableLiveData<>();
	private final MutableLiveData<String> mErrorLiveData = new MutableLiveData<>();
	private final SingleLiveEvent<Void> mCancelledEvent = new SingleLiveEvent<>();

	@Inject
	FilesDownloadViewModel(final FsManager manager,
						   @Named("busy") final MutableLiveData<Boolean> state) {
		super(state);
		mManager = manager;
	}

	@NonNull
	public LiveData<Integer> getProgress() {
		return mProgressLiveData;
	}

	@NonNull
	public LiveData<byte[]> getResponse() {
		return mResponseLiveData;
	}

	@NonNull
	public LiveData<String> getError() {
		return mErrorLiveData;
	}

	@NonNull
	public LiveData<Void> getCancelledEvent() {
		return mCancelledEvent;
	}

	public void download(final String path) {
		setBusy();
		final McuMgrTransport transport = mManager.getTransporter();
		if (transport instanceof McuMgrBleTransport) {
			((McuMgrBleTransport) transport).requestConnPriority(ConnectionPriorityRequest.CONNECTION_PRIORITY_HIGH);
		}
		mManager.download(path, this);
	}

	public void pause() {
		if (mManager.getState() == FsManager.STATE_DOWNLOADING) {
			mManager.pauseTransfer();
			setReady();
		}
	}

	public void resume() {
		if (mManager.getState() == FsManager.STATE_PAUSED) {
			setBusy();
			mManager.continueTransfer();
		}
	}

	public void cancel() {
		mManager.cancelTransfer();
	}

	@Override
	public void onProgressChanged(final int bytesDownloaded, final int imageSize,
								  final long timestamp) {
		// Convert to percent
		mProgressLiveData.postValue((int) (bytesDownloaded * 100.f / imageSize));
	}

	@Override
	public void onDownloadCanceled() {
		mProgressLiveData.postValue(0);
		mCancelledEvent.post();
		postReady();
	}

	@Override
	public void onDownloadFailed(@NonNull final McuMgrException error) {
		mProgressLiveData.postValue(0);
		if (error instanceof McuMgrErrorException) {
			final McuMgrErrorCode code = ((McuMgrErrorException) error).getCode();
			if (code == McuMgrErrorCode.UNKNOWN) {
				mResponseLiveData.postValue(null); // File not found
				postReady();
				return;
			}
		}
		mErrorLiveData.postValue(error.getMessage());
		postReady();
	}

	@Override
	public void onDownloadFinished(@NonNull final String name, @NonNull final byte[] data) {
		mProgressLiveData.postValue(0);
		mResponseLiveData.postValue(data);
		postReady();
	}
}
