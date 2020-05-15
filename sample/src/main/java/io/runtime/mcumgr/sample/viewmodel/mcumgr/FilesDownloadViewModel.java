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

import org.jetbrains.annotations.NotNull;

import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.ble.McuMgrBleTransport;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.managers.FsManager;
import io.runtime.mcumgr.sample.viewmodel.SingleLiveEvent;
import io.runtime.mcumgr.transfer.DownloadCallback;
import io.runtime.mcumgr.transfer.TransferController;
import no.nordicsemi.android.ble.ConnectionPriorityRequest;

@SuppressWarnings("unused")
public class FilesDownloadViewModel extends McuMgrViewModel implements DownloadCallback {
    private final FsManager mManager;
    private TransferController mController;

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
        if (mController != null) {
            return;
        }
        setBusy();
        final McuMgrTransport transport = mManager.getTransporter();
        if (transport instanceof McuMgrBleTransport) {
            ((McuMgrBleTransport) transport).requestConnPriority(ConnectionPriorityRequest.CONNECTION_PRIORITY_HIGH);
        }
        mController = mManager.fileDownload(path, this);
    }

    public void pause() {
        final TransferController controller = mController;
        if (controller != null) {
            controller.pause();
            setReady();
        }
    }

    public void resume() {
        final TransferController controller = mController;
        if (controller != null) {
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
    public void onDownloadProgressChanged(final int current, final int total, final long timestamp) {
        // Convert to percent
        mProgressLiveData.postValue((int) (current * 100.f / total));
    }

    @Override
    public void onDownloadFailed(@NonNull final McuMgrException error) {
        mController = null;
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
    public void onDownloadCanceled() {
        mController = null;
        mProgressLiveData.postValue(0);
        mCancelledEvent.post();
        postReady();
    }

    @Override
    public void onDownloadCompleted(@NotNull final byte[] data) {
        mController = null;
        mProgressLiveData.postValue(0);
        mResponseLiveData.postValue(data);
        postReady();
    }
}
