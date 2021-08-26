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
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.ble.McuMgrBleTransport;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.managers.FsManager;
import io.runtime.mcumgr.sample.viewmodel.SingleLiveEvent;
import io.runtime.mcumgr.transfer.TransferController;
import io.runtime.mcumgr.transfer.UploadCallback;
import no.nordicsemi.android.ble.ConnectionPriorityRequest;

public class FilesUploadViewModel extends McuMgrViewModel implements UploadCallback {
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
    private TransferController mController;

    private final MutableLiveData<State> mStateLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> mProgressLiveData = new MutableLiveData<>();
    private final MutableLiveData<McuMgrException> mErrorLiveData = new MutableLiveData<>();
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
    public LiveData<McuMgrException> getError() {
        return mErrorLiveData;
    }

    @NonNull
    public LiveData<Void> getCancelledEvent() {
        return mCancelledEvent;
    }

    public void upload(final String path, final byte[] data) {
        if (mController != null) {
            return;
        }
        setBusy();
        mStateLiveData.setValue(State.UPLOADING);
        final McuMgrTransport transport = mManager.getTransporter();
        if (transport instanceof McuMgrBleTransport) {
            ((McuMgrBleTransport) transport).requestConnPriority(ConnectionPriorityRequest.CONNECTION_PRIORITY_HIGH);
        }
        mController = mManager.fileUpload(path, data, this);
    }

    public void pause() {
        final TransferController controller = mController;
        if (controller != null) {
            mStateLiveData.setValue(State.PAUSED);
            controller.pause();
            setReady();
        }
    }

    public void resume() {
        final TransferController controller = mController;
        if (controller != null) {
            mStateLiveData.setValue(State.UPLOADING);
            setBusy();
            controller.resume();
        }
    }

    public void cancel() {
        final TransferController controller = mController;
        if (controller != null) {
            controller.cancel();
        }
    }

    @Override
    public void onUploadProgressChanged(final int current, final int total, final long timestamp) {
        // Convert to percent
        mProgressLiveData.postValue((int) (current * 100.f / total));
    }

    @Override
    public void onUploadFailed(@NonNull final McuMgrException error) {
        mController = null;
        mProgressLiveData.postValue(0);
        mErrorLiveData.postValue(error);
        postReady();
    }

    @Override
    public void onUploadCanceled() {
        mController = null;
        mProgressLiveData.postValue(0);
        mStateLiveData.postValue(State.IDLE);
        mCancelledEvent.post();
        postReady();
    }

    @Override
    public void onUploadCompleted() {
        mController = null;
        mProgressLiveData.postValue(0);
        mStateLiveData.postValue(State.COMPLETE);
        postReady();
    }
}
