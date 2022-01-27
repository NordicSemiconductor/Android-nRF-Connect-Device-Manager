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
import io.runtime.mcumgr.transfer.TransferController;
import io.runtime.mcumgr.transfer.UploadCallback;
import no.nordicsemi.android.ble.ConnectionPriorityRequest;

public class ImageUploadViewModel extends McuMgrViewModel implements UploadCallback {
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

    private final ImageManager manager;
    private TransferController controller;

    private final MutableLiveData<State> stateLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> progressLiveData = new MutableLiveData<>();
    private final MutableLiveData<Float> transferSpeedLiveData = new MutableLiveData<>();
    private final SingleLiveEvent<McuMgrException> errorLiveData = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> cancelledEvent = new SingleLiveEvent<>();

    private long uploadStartTimestamp;
    private int initialBytes;

    @Inject
    ImageUploadViewModel(final ImageManager manager,
                         @Named("busy") final MutableLiveData<Boolean> state) {
        super(state);
        this.manager = manager;
        this.stateLiveData.setValue(State.IDLE);
        this.progressLiveData.setValue(0);
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

    public void upload(@NonNull final byte[] data, final int image) {
        if (controller != null) {
            return;
        }
        setBusy();
        stateLiveData.setValue(State.VALIDATING);

        byte[] hash;
        try {
            hash = McuMgrImage.getHash(data);
        } catch (final McuMgrException e) {
            errorLiveData.setValue(e);
            return;
        }

        requestHighConnectionPriority();
        manager.list(new McuMgrCallback<McuMgrImageStateResponse>() {
            @Override
            public void onResponse(@NonNull final McuMgrImageStateResponse response) {
                // Check if the fw has already been sent before.
                McuMgrImageStateResponse.ImageSlot theSameImage = null;
                for (final McuMgrImageStateResponse.ImageSlot image: response.images) {
                    if (Arrays.equals(hash, image.hash)) {
                        theSameImage = image;
                        break;
                    }
                }
                // If yes, no need to send again.
                if (theSameImage != null) {
                    if (theSameImage.slot == 0) {
                        errorLiveData.postValue(new McuMgrException("Firmware already active."));
                    } else {
                        // Firmware is identical to one on slot 1. No need to send anything.
                        stateLiveData.postValue(State.COMPLETE);
                    }
                    postReady();
                    return;
                }
                // Otherwise, send the firmware. This may return NO MEMORY error if slot 1 is
                // filled with an image with pending or confirmed flags set.
                stateLiveData.postValue(State.UPLOADING);
                initialBytes = 0;
                setLoggingEnabled(false);
                controller = manager.imageUpload(data, image,ImageUploadViewModel.this);
            }

            @Override
            public void onError(@NonNull final McuMgrException error) {
                errorLiveData.postValue(error);
                postReady();
            }
        });
    }

    public void pause() {
        final TransferController controller = this.controller;
        if (controller != null) {
            stateLiveData.setValue(State.PAUSED);
            setLoggingEnabled(true);
            controller.pause();
            setReady();
        }
    }

    public void resume() {
        final TransferController controller = this.controller;
        if (controller != null) {
            setBusy();
            stateLiveData.setValue(State.UPLOADING);
            initialBytes = 0;
            setLoggingEnabled(false);
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
    public void onUploadProgressChanged(final int bytesSent, final int imageSize, final long timestamp) {
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
        progressLiveData.postValue((int) (bytesSent * 100.f / imageSize));
    }

    @Override
    public void onUploadFailed(@NonNull final McuMgrException error) {
        controller = null;
        progressLiveData.postValue(0);
        errorLiveData.postValue(error);
        setLoggingEnabled(true);
        postReady();
    }

    @Override
    public void onUploadCanceled() {
        controller = null;
        progressLiveData.postValue(0);
        stateLiveData.postValue(State.IDLE);
        cancelledEvent.post();
        setLoggingEnabled(true);
        postReady();
    }

    @Override
    public void onUploadCompleted() {
        controller = null;
        progressLiveData.postValue(0);
        stateLiveData.postValue(State.COMPLETE);
        setLoggingEnabled(true);
        postReady();
    }

    private void requestHighConnectionPriority() {
        final McuMgrTransport transporter = manager.getTransporter();
        if (transporter instanceof McuMgrBleTransport) {
            final McuMgrBleTransport bleTransporter = (McuMgrBleTransport) transporter;
            bleTransporter.requestConnPriority(ConnectionPriorityRequest.CONNECTION_PRIORITY_HIGH);
        }
    }

    private void setLoggingEnabled(final boolean enabled) {
        final McuMgrTransport transporter = manager.getTransporter();
        if (transporter instanceof McuMgrBleTransport) {
            final McuMgrBleTransport bleTransporter = (McuMgrBleTransport) transporter;
            bleTransporter.setLoggingEnabled(enabled);
        }
    }
}
