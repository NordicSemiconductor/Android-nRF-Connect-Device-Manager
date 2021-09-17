/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.viewmodel.mcumgr;

import android.util.Pair;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.ble.McuMgrBleTransport;
import io.runtime.mcumgr.dfu.FirmwareUpgradeCallback;
import io.runtime.mcumgr.dfu.FirmwareUpgradeController;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.image.McuMgrImage;
import io.runtime.mcumgr.sample.utils.ZipPackage;
import io.runtime.mcumgr.sample.viewmodel.SingleLiveEvent;
import no.nordicsemi.android.ble.ConnectionPriorityRequest;
import timber.log.Timber;

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
    private final MutableLiveData<Float> mTransferSpeedLiveData = new MutableLiveData<>();
    private final SingleLiveEvent<McuMgrException> mErrorLiveData = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> mCancelledEvent = new SingleLiveEvent<>();

    private long mUploadStartTimestamp;
    private int mInitialBytes;

    @Inject
    ImageUpgradeViewModel(final FirmwareUpgradeManager manager,
                          @Named("busy") final MutableLiveData<Boolean> state) {
        super(state);
        mManager = manager;
        mManager.setFirmwareUpgradeCallback(this);

        // mRF52840, due to how the flash memory works, requires ~20 sec to erase images.
        mManager.setEstimatedSwapTime(10000);

        // Window upload is experimental and seems not to work well.
        // Each packets sent gets SEQ number assigned. Each response has the same sequence number.
        // It should be possible to send multiple packets quickly, which should be processed and
        // acknowledged when done. However, on Zephyr implementation the requests are not buffered,
        // so all replies get offset set to number of bytes processed, making the library to resend
        // a lot of packets. Also, with window upload pause and resume throw an exception.

        // mManager.setWindowUploadCapacity(32);

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

    /**
     * Returns current transfer speed in KB/s.
     */
    @NonNull
    public LiveData<Float> getTransferSpeed() {
        return mTransferSpeedLiveData;
    }

    @NonNull
    public LiveData<McuMgrException> getError() {
        return mErrorLiveData;
    }

    @NonNull
    public LiveData<Void> getCancelledEvent() {
        return mCancelledEvent;
    }

    public void upgrade(@NonNull final byte[] data,
                        @NonNull final FirmwareUpgradeManager.Mode mode,
                        final boolean eraseSettings) {
        List<Pair<Integer, byte[]>> images;
        try {
            // Check if the BIN file is valid.
            McuMgrImage.getHash(data);
            images = Collections.singletonList(new Pair<>(0, data));
        } catch (final Exception e) {
            try {
                final ZipPackage zip = new ZipPackage(data);
                images = zip.getBinaries();
            } catch (final Exception e1) {
                mErrorLiveData.setValue(new McuMgrException("Invalid image file."));
                return;
            }
        }
        try {
            final McuMgrTransport transport = mManager.getTransporter();
            if (transport instanceof McuMgrBleTransport) {
                ((McuMgrBleTransport) transport).requestConnPriority(ConnectionPriorityRequest.CONNECTION_PRIORITY_HIGH);
            }
            mManager.setMode(mode);
            mManager.start(images, eraseSettings);
        } catch (final McuMgrException e) {
            // TODO Externalize the text
            mErrorLiveData.setValue(new McuMgrException("Invalid image file."));
        }
    }

    public void pause() {
        if (mManager.isInProgress()) {
            mStateLiveData.postValue(State.PAUSED);
            mManager.pause();
            Timber.i("Upload paused");
            setReady();
        }
    }

    public void resume() {
        if (mManager.isPaused()) {
            setBusy();
            mStateLiveData.postValue(State.UPLOADING);
            Timber.i("Upload resumed");
            mInitialBytes = 0;
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
        // Enable logging for BLE transport
        final McuMgrTransport transporter = mManager.getTransporter();
        if (transporter instanceof McuMgrBleTransport) {
            final McuMgrBleTransport bleTransporter = (McuMgrBleTransport) transporter;
            bleTransporter.setLoggingEnabled(newState != FirmwareUpgradeManager.State.UPLOAD);
        }
        switch (newState) {
            case UPLOAD:
                Timber.i("Uploading firmware...");
                mInitialBytes = 0;
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
        if (mInitialBytes == 0) {
            mUploadStartTimestamp = timestamp;
            mInitialBytes = bytesSent;
        } else {
            final int bytesSentSinceUploadStarted = bytesSent - mInitialBytes;
            final long timeSinceUploadStarted = timestamp - mUploadStartTimestamp;
            // bytes / ms = KB/s
            mTransferSpeedLiveData.postValue((float) bytesSentSinceUploadStarted / (float) timeSinceUploadStarted);
        }
        // When done, reset the counter.
        if (bytesSent == imageSize) {
            mInitialBytes = 0;
        }
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
        mErrorLiveData.postValue(error);
        postReady();
    }
}
