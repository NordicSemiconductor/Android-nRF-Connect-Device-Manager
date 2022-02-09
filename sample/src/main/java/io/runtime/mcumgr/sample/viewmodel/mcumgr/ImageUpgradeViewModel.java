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

    private final FirmwareUpgradeManager manager;

    private final MutableLiveData<State> stateLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> progressLiveData = new MutableLiveData<>();
    private final MutableLiveData<Float> transferSpeedLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> advancedSettingsExpanded = new MutableLiveData<>();
    private final SingleLiveEvent<McuMgrException> errorLiveData = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> cancelledEvent = new SingleLiveEvent<>();

    private long uploadStartTimestamp;
    private int initialBytes;

    @Inject
    ImageUpgradeViewModel(final FirmwareUpgradeManager manager,
                          @Named("busy") final MutableLiveData<Boolean> state) {
        super(state);
        this.manager = manager;
        this.manager.setFirmwareUpgradeCallback(this);

        stateLiveData.setValue(State.IDLE);
        progressLiveData.setValue(0);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        manager.setFirmwareUpgradeCallback(null);
    }

    @NonNull
    public LiveData<Boolean> getAdvancedSettingsState() {
        return advancedSettingsExpanded;
    }

    public void setAdvancedSettingsExpanded(final boolean expanded) {
        advancedSettingsExpanded.setValue(expanded);
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

    public void upgrade(@NonNull final byte[] data,
                        @NonNull final FirmwareUpgradeManager.Mode mode,
                        final boolean eraseSettings,
                        final int estimatedSwapTime,
                        final int windowCapacity,
                        final int memoryAlignment) {
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
                errorLiveData.setValue(new McuMgrException("Invalid image file."));
                return;
            }
        }
        try {
            requestHighConnectionPriority();

            // Set the upgrade mode.
            manager.setMode(mode);
            // rF52840, due to how the flash memory works, requires ~20 sec to erase images.
            manager.setEstimatedSwapTime(estimatedSwapTime);
            // Set the window capacity. Values > 1 enable a new implementation for uploading
            // the images, which makes use of SMP pipelining feature.
            // The app will send this many packets immediately, without waiting for notification
            // confirming each packet. This value should be lower or equal to MCUMGR_BUF_COUNT
            // (https://github.com/zephyrproject-rtos/zephyr/blob/bd4ddec0c8c822bbdd420bd558b62c1d1a532c16/subsys/mgmt/mcumgr/Kconfig#L550)
            // parameter in KConfig in NCS / Zephyr configuration and should also be supported
            // on Mynewt devices.
            // Mind, that in Zephyr, before https://github.com/zephyrproject-rtos/zephyr/pull/41959
            // was merged, the device required data to be sent with memory alignment. Otherwise,
            // the device would ignore uneven bytes and reply with lower than expected offset
            // causing multiple packets to be sent again dropping the speed instead of increasing it.
            manager.setWindowUploadCapacity(windowCapacity);
            // Set the selected memory alignment. In the app this defaults to 4 to match Nordic
            // devices, but can be modified in the UI.
            manager.setMemoryAlignment(memoryAlignment);

            manager.start(images, eraseSettings);
        } catch (final McuMgrException e) {
            // TODO Externalize the text
            errorLiveData.setValue(new McuMgrException("Invalid image file."));
        }
    }

    public void pause() {
        if (manager.isInProgress()) {
            stateLiveData.postValue(State.PAUSED);
            manager.pause();
            Timber.i("Upload paused");
            setLoggingEnabled(true);
            setReady();
        }
    }

    public void resume() {
        if (manager.isPaused()) {
            setBusy();
            stateLiveData.postValue(State.UPLOADING);
            Timber.i("Upload resumed");
            initialBytes = 0;
            setLoggingEnabled(false);
            manager.resume();
        }
    }

    public void cancel() {
        manager.cancel();
    }

    @Override
    public void onUpgradeStarted(final FirmwareUpgradeController controller) {
        postBusy();
        stateLiveData.setValue(State.VALIDATING);
    }

    @Override
    public void onStateChanged(
            final FirmwareUpgradeManager.State prevState,
            final FirmwareUpgradeManager.State newState)
    {
        setLoggingEnabled(newState != FirmwareUpgradeManager.State.UPLOAD);
        switch (newState) {
            case UPLOAD:
                Timber.i("Uploading firmware...");
                initialBytes = 0;
                stateLiveData.postValue(State.UPLOADING);
                break;
            case TEST:
                stateLiveData.postValue(State.TESTING);
                break;
            case CONFIRM:
                stateLiveData.postValue(State.CONFIRMING);
                break;
            case RESET:
                stateLiveData.postValue(State.RESETTING);
                break;
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
        // When done, reset the counter.
        if (bytesSent == imageSize) {
            Timber.i("Image (%d bytes) sent in %d ms (avg speed: %f kB/s)",
                    imageSize - initialBytes,
                    timestamp - uploadStartTimestamp,
                    (float) (imageSize - initialBytes) / (float) (timestamp - uploadStartTimestamp)
            );
            initialBytes = 0;
        }
        // Convert to percent
        progressLiveData.postValue((int) (bytesSent * 100.f / imageSize));
    }

    @Override
    public void onUpgradeCompleted() {
        progressLiveData.postValue(0);
        stateLiveData.postValue(State.COMPLETE);
        Timber.i("Upgrade complete");
        setLoggingEnabled(true);
        postReady();
    }

    @Override
    public void onUpgradeCanceled(final FirmwareUpgradeManager.State state) {
        progressLiveData.postValue(0);
        stateLiveData.postValue(State.IDLE);
        cancelledEvent.post();
        Timber.w("Upgrade cancelled");
        setLoggingEnabled(true);
        postReady();
    }

    @Override
    public void onUpgradeFailed(final FirmwareUpgradeManager.State state, final McuMgrException error) {
        progressLiveData.postValue(0);
        errorLiveData.postValue(error);
        setLoggingEnabled(true);
        Timber.e(error, "Upgrade failed");
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
