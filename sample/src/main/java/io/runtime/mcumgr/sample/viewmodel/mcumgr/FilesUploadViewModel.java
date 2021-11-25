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

    private final FsManager manager;
    private TransferController controller;

    private final MutableLiveData<State> stateLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> progressLiveData = new MutableLiveData<>();
    private final MutableLiveData<Float> transferSpeedLiveData = new MutableLiveData<>();
    private final MutableLiveData<McuMgrException> errorLiveData = new MutableLiveData<>();
    private final SingleLiveEvent<Void> cancelledEvent = new SingleLiveEvent<>();

    private long uploadStartTimestamp;
    private int initialBytes;

    @Inject
    FilesUploadViewModel(final FsManager manager,
                         @Named("busy") final MutableLiveData<Boolean> state) {
        super(state);
        this.manager = manager;
        this.stateLiveData.setValue(State.IDLE);
    }

    @NonNull
    public LiveData<State> getState() {
        return stateLiveData;
    }

    @NonNull
    public LiveData<Integer> getProgress() {
        return progressLiveData;
    }

    /**
     * Returns current transfer speed in KB/s.
     */
    @NonNull
    public LiveData<Float> getTransferSpeed() {
        return transferSpeedLiveData;
    }

    @NonNull
    public LiveData<McuMgrException> getError() {
        return errorLiveData;
    }

    @NonNull
    public LiveData<Void> getCancelledEvent() {
        return cancelledEvent;
    }

    public void upload(final String path, final byte[] data) {
        if (controller != null) {
            return;
        }
        setBusy();
        stateLiveData.setValue(State.UPLOADING);
        final McuMgrTransport transport = manager.getTransporter();
        if (transport instanceof McuMgrBleTransport) {
            ((McuMgrBleTransport) transport).requestConnPriority(ConnectionPriorityRequest.CONNECTION_PRIORITY_HIGH);
        }
        initialBytes = 0;
        controller = manager.fileUpload(path, data, this);
    }

    public void pause() {
        final TransferController controller = this.controller;
        if (controller != null) {
            stateLiveData.setValue(State.PAUSED);
            controller.pause();
            setReady();
        }
    }

    public void resume() {
        final TransferController controller = this.controller;
        if (controller != null) {
            stateLiveData.setValue(State.UPLOADING);
            setBusy();
            initialBytes = 0;
            controller.resume();
        }
    }

    public void cancel() {
        final TransferController controller = this.controller;
        if (controller != null) {
            controller.cancel();
        }
    }

    @Override
    public void onUploadProgressChanged(final int bytesSent, final int fileSize, final long timestamp) {
        if (initialBytes == 0) {
            uploadStartTimestamp = timestamp;
            initialBytes = bytesSent;
        } else {
            final int bytesSentSinceUploadStarted = bytesSent - initialBytes;
            final long timeSinceUploadStarted = timestamp - uploadStartTimestamp;
            // bytes / ms = KB/s
            transferSpeedLiveData.postValue((float) bytesSentSinceUploadStarted / (float) timeSinceUploadStarted);
        }
        // Convert to percent
        progressLiveData.postValue((int) (bytesSent * 100.f / fileSize));
    }

    @Override
    public void onUploadFailed(@NonNull final McuMgrException error) {
        controller = null;
        progressLiveData.postValue(0);
        errorLiveData.postValue(error);
        postReady();
    }

    @Override
    public void onUploadCanceled() {
        controller = null;
        progressLiveData.postValue(0);
        stateLiveData.postValue(State.IDLE);
        cancelledEvent.post();
        postReady();
    }

    @Override
    public void onUploadCompleted() {
        controller = null;
        progressLiveData.postValue(0);
        stateLiveData.postValue(State.COMPLETE);
        postReady();
    }
}
