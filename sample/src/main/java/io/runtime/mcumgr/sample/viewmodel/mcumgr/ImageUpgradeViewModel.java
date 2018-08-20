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

import io.runtime.mcumgr.dfu.FirmwareUpgradeCallback;
import io.runtime.mcumgr.dfu.FirmwareUpgradeController;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.sample.viewmodel.SingleLiveEvent;

public class ImageUpgradeViewModel extends McuMgrViewModel implements FirmwareUpgradeCallback {
	public enum State {
		IDLE,
		VALIDATING,
		UPLOADING,
		PAUSED,
		TESTING,
		CONFIRMING,
		RESETTING,
		COMPLETE;

		public boolean inProgress() {
			return this != IDLE && this != COMPLETE;
		}

		public boolean canPauseOrResume() {
			return this == UPLOADING || this == PAUSED;
		}

		public boolean canCancel() {
			return this == VALIDATING || this == UPLOADING || this == PAUSED;
		}
	}

	private final FirmwareUpgradeManager mManager;

	private final MutableLiveData<State> mStateLiveData = new MutableLiveData<>();
	private final MutableLiveData<Integer> mProgressLiveData = new MutableLiveData<>();
	private final SingleLiveEvent<String> mErrorLiveData = new SingleLiveEvent<>();
	private final SingleLiveEvent<Void> mCancelledEvent = new SingleLiveEvent<>();

	@Inject
	ImageUpgradeViewModel(final FirmwareUpgradeManager manager,
						  @Named("busy") final MutableLiveData<Boolean> state) {
		super(state);
		mManager = manager;
		mManager.setFirmwareUpgradeCallback(this);
		mStateLiveData.setValue(State.IDLE);
		mProgressLiveData.setValue(0);
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

	public void upgrade(@NonNull final byte[] data, @NonNull final FirmwareUpgradeManager.Mode mode) {
		try {
			mManager.setMode(mode);
			mManager.start(data);
		} catch (final McuMgrException e) {
			// TODO Externalize the text
			mErrorLiveData.setValue("Invalid image file.");
		}
	}

	public void pause() {
		if (mManager.isInProgress()) {
			mStateLiveData.postValue(State.PAUSED);
			mManager.pause();
			setReady();
		}
	}

	public void resume() {
		if (mManager.isPaused()) {
			setBusy();
			mStateLiveData.postValue(State.UPLOADING);
			mManager.resume();
		}
	}

	public void cancel() {
		mManager.cancel();
	}


	@Override
	public void onUpgradeStarted(final FirmwareUpgradeController controller) {
		postBusy();
		mStateLiveData.setValue(State.VALIDATING);
	}

	@Override
	public void onStateChanged(final FirmwareUpgradeManager.State prevState, final FirmwareUpgradeManager.State newState) {
		switch (newState) {
			case UPLOAD:
				mStateLiveData.postValue(State.UPLOADING);
				break;
			case TEST:
				mStateLiveData.postValue(State.TESTING);
				break;
			case CONFIRM:
				mStateLiveData.postValue(State.CONFIRMING);
				break;
			case RESET:
				mStateLiveData.postValue(State.RESETTING);
				break;
		}
	}

	@Override
	public void onUploadProgressChanged(final int bytesSent, final int imageSize, final long timestamp) {
		// Convert to percent
		mProgressLiveData.postValue((int) (bytesSent * 100.f / imageSize));
	}

	@Override
	public void onUpgradeCompleted() {
		mProgressLiveData.postValue(0);
		mStateLiveData.postValue(State.COMPLETE);
		postReady();
	}

	@Override
	public void onUpgradeCanceled(final FirmwareUpgradeManager.State state) {
		mProgressLiveData.postValue(0);
		mStateLiveData.postValue(State.IDLE);
		mCancelledEvent.post();
		postReady();
	}

	@Override
	public void onUpgradeFailed(final FirmwareUpgradeManager.State state, final McuMgrException error) {
		mProgressLiveData.postValue(0);
		mErrorLiveData.postValue(error.getMessage());
		postReady();
	}
}
