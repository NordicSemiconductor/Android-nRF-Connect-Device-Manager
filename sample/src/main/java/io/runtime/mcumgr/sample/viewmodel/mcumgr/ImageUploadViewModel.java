/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.viewmodel.mcumgr;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.ble.McuMgrBleTransport;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.image.McuMgrImage;
import io.runtime.mcumgr.managers.ImageManager;
import io.runtime.mcumgr.response.img.McuMgrImageStateResponse;
import io.runtime.mcumgr.sample.viewmodel.SingleLiveEvent;
import no.nordicsemi.android.ble.ConnectionPriorityRequest;

public class ImageUploadViewModel extends McuMgrViewModel implements ImageManager.ImageUploadCallback {
    public enum State {
        IDLE,
        VALIDATING,
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

    private final ImageManager mManager;

    private final MutableLiveData<State> mStateLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> mProgressLiveData = new MutableLiveData<>();
    private final SingleLiveEvent<String> mErrorLiveData = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> mCancelledEvent = new SingleLiveEvent<>();

    @Inject
    ImageUploadViewModel(final ImageManager manager,
                         @Named("busy") final MutableLiveData<Boolean> state) {
        super(state);
        mManager = manager;
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

    public void upload(@NonNull final byte[] data) {
        setBusy();
        mStateLiveData.setValue(State.VALIDATING);

        byte[] hash;
        try {
            hash = McuMgrImage.getHash(data);
        } catch (final McuMgrException e) {
            // TODO Externalize the text
            mErrorLiveData.setValue("Invalid image file.");
            return;
        }

        final McuMgrTransport transport = mManager.getTransporter();
        if (transport instanceof McuMgrBleTransport) {
            ((McuMgrBleTransport) transport).requestConnPriority(ConnectionPriorityRequest.CONNECTION_PRIORITY_HIGH);
        }
        mManager.list(new McuMgrCallback<McuMgrImageStateResponse>() {
            @Override
            public void onResponse(@NonNull final McuMgrImageStateResponse response) {
                // Check if the new firmware is different than the active one.
                if (response.images.length > 0 && Arrays.equals(hash, response.images[0].hash)) {
                    // TODO Externalize the text
                    mErrorLiveData.setValue("Firmware already active.");
                    postReady();
                    return;
                }

                // Check if the new firmware was already sent.
                if (response.images.length > 1 && Arrays.equals(hash, response.images[1].hash)) {
                    // Firmware is identical to one on slot 1. No need to send anything.
                    mStateLiveData.setValue(State.COMPLETE);
                    postReady();
                    return;
                }

                // Send the firmware.
                mStateLiveData.postValue(State.UPLOADING);
                mManager.upload(data, ImageUploadViewModel.this);
            }

            @Override
            public void onError(@NonNull final McuMgrException error) {
                mErrorLiveData.postValue(error.getMessage());
                postReady();
            }
        });
    }

    public void pause() {
        if (mManager.getUploadState() == ImageManager.STATE_UPLOADING) {
            mStateLiveData.setValue(State.PAUSED);
            mManager.pauseUpload();
            setReady();
        }
    }

    public void resume() {
        if (mManager.getUploadState() == ImageManager.STATE_PAUSED) {
            setBusy();
            mStateLiveData.setValue(State.UPLOADING);
            mManager.continueUpload();
        }
    }

    public void cancel() {
        mManager.cancelUpload();
    }

    @Override
    public void onProgressChanged(final int bytesSent, final int imageSize, final long timestamp) {
        // Convert to percent
        mProgressLiveData.postValue((int) (bytesSent * 100.f / imageSize));
    }

    @Override
    public void onUploadFailed(@NonNull final McuMgrException error) {
        mProgressLiveData.postValue(0);
        mErrorLiveData.postValue(error.getMessage());
        postReady();
    }

    @Override
    public void onUploadCanceled() {
        mProgressLiveData.postValue(0);
        mStateLiveData.postValue(State.IDLE);
        mCancelledEvent.post();
        postReady();
    }

    @Override
    public void onUploadFinished() {
        mProgressLiveData.postValue(0);
        mStateLiveData.postValue(State.COMPLETE);
        postReady();
    }
}
