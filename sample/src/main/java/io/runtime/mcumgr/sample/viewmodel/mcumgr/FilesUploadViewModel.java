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

import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.managers.FsManager;
import io.runtime.mcumgr.sample.viewmodel.SingleLiveEvent;

public class FilesUploadViewModel extends McuMgrViewModel implements FsManager.FileUploadCallback {
	public enum State {
		IDLE,
		UPLOADING,
		PAUSED,
		COMPLETE;

		public boolean inProgress() {
			return this != IDLE && this != COMPLETE;
		}

		public boolean canPauseOrResume() {
			return this == UPLOADING || this == PAUSED;
		}

		public boolean canCancel() {
			return this == UPLOADING || this == PAUSED;
		}
	}

	private final FsManager mManager;

	private final MutableLiveData<State> mStateLiveData = new MutableLiveData<>();
	private final MutableLiveData<Integer> mProgressLiveData = new MutableLiveData<>();
	private final MutableLiveData<String> mErrorLiveData = new MutableLiveData<>();
	private final SingleLiveEvent<Void> mCancelledEvent = new SingleLiveEvent<>();

	@Inject
    FilesUploadViewModel(final FsManager manager,
                         @Named("busy") final MutableLiveData<Boolean> state) {
		super(state);
		mStateLiveData.setValue(State.IDLE);
		mManager = manager;
	}

	@NonNull
	public LiveData<State> getState() {
		return mStateLiveData;
	}

	@NonNull
	public LiveData<Integer> getProgress() {
		return mProgressLiveData;
	}

	@NonNull
	public LiveData<String> getError() {
		return mErrorLiveData;
	}

	@NonNull
	public LiveData<Void> getCancelledEvent() {
		return mCancelledEvent;
	}

	public void upload(final String path, final byte[] data) {
		setBusy();
		mStateLiveData.setValue(State.UPLOADING);
		mManager.upload(path, data, this);
	}

	public void pause() {
		if (mManager.getState() == FsManager.STATE_UPLOADING) {
			mStateLiveData.setValue(State.PAUSED);
			mManager.pauseTransfer();
			setReady();
		}
	}

	public void resume() {
		if (mManager.getState() == FsManager.STATE_PAUSED) {
			mStateLiveData.setValue(State.UPLOADING);
			setBusy();
			mManager.continueTransfer();
		}
	}

	public void cancel() {
		mManager.cancelTransfer();
	}

	@Override
	public void onProgressChange(final int bytesSent, final int imageSize, final long timestamp) {
		// Convert to percent
		mProgressLiveData.postValue((int) (bytesSent * 100.f / imageSize));
	}

	@Override
	public void onUploadFail(@NonNull final McuMgrException error) {
		mProgressLiveData.postValue(0);
		mErrorLiveData.postValue(error.getMessage());
		postReady();
	}

	@Override
	public void onUploadCancel() {
		mProgressLiveData.postValue(0);
		mStateLiveData.postValue(State.IDLE);
		mCancelledEvent.post();
		postReady();
	}

	@Override
	public void onUploadFinish() {
		mProgressLiveData.postValue(0);
		mStateLiveData.postValue(State.COMPLETE);
		postReady();
	}
}
