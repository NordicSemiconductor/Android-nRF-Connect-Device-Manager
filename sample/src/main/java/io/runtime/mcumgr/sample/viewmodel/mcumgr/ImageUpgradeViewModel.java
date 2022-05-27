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
import kotlin.Triple;
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

    public static class ThroughputData {
        public int progress;
        public float instantaneousThroughput;
        public float averageThroughput;

        public ThroughputData(final int progress, final float instantaneousThroughput, final float averageThroughput) {
            this.progress = progress;
            this.instantaneousThroughput = instantaneousThroughput;
            this.averageThroughput = averageThroughput;
        }
    }

    private final FirmwareUpgradeManager manager;

    private final MutableLiveData<State> stateLiveData = new MutableLiveData<>();
    private final MutableLiveData<ThroughputData> progressLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> advancedSettingsExpanded = new MutableLiveData<>();
    private final SingleLiveEvent<McuMgrException> errorLiveData = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void> cancelledEvent = new SingleLiveEvent<>();

    private long uploadStartTimestamp, lastProgressChangeTimestamp;
    private int initialBytesSent, lastProgressChangeBytesSent, lastProgress;
    /** A value indicating that the upload has not been started before. */
    private final static int NOT_STARTED = -1;
    /**
     * The minimum time interval prevents from sharp peaks on the throughput graph.
     * Otherwise it may happen that the bytes are sent in a single connection interval and the
     * time, for which we divide the number of bytes, is very low and the throughput skyrockets.
     */
    private static final float MIN_INTERVAL = 36.0f;

    @Inject
    ImageUpgradeViewModel(final FirmwareUpgradeManager manager,
                          @Named("busy") final MutableLiveData<Boolean> state) {
        super(state);
        this.manager = manager;
        this.manager.setFirmwareUpgradeCallback(this);

        stateLiveData.setValue(State.IDLE);
        progressLiveData.setValue(null);
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

    /**
     * Returns current transfer speed in KB/s.
     */
    @NonNull
    public LiveData<ThroughputData> getProgress() {
        return progressLiveData;
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
            initialBytesSent = NOT_STARTED;
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
        progressLiveData.setValue(null);
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
                initialBytesSent = NOT_STARTED;
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
        // Calculate the current upload progress.
        final int progress = (int) (bytesSent * 100.f / imageSize);

        float averageThroughput = 0.0f;
        float instantaneousThroughput = 0.0f;

        // Check if this is the first time this method is called since:
        // - the start of an upload
        // - after resume
        if (initialBytesSent == NOT_STARTED) {
            // If a new image started being sending, clear the progress graph.
            if (lastProgress > progress) {
                progressLiveData.postValue(null);
            }
            lastProgress = progress;

            // To calculate the throughput it is necessary to store the initial timestamp and
            // the number of bytes sent so far. Mind, that the upload may be resumed from any point,
            // not necessarily from the beginning.
            uploadStartTimestamp = timestamp;
            initialBytesSent = bytesSent;
            // The instantaneous throughput is calculated each time the integer percentage value
            // of the progress changes.
            lastProgressChangeTimestamp = timestamp;
            lastProgressChangeBytesSent = bytesSent;
        } else {
            // Calculate the average throughout.
            // This is done by diving number of bytes sent since upload has been started (or resumed)
            // by the time since that moment. The minimum time of MIN_INTERVAL ms prevents from
            // graph peaks that are not seen in reality during tests.
            final float bytesSentSinceUploadStarted = bytesSent - initialBytesSent;
            final float timeSinceUploadStarted = Math.max(MIN_INTERVAL, timestamp - uploadStartTimestamp);
            averageThroughput = bytesSentSinceUploadStarted / timeSinceUploadStarted; // bytes / ms = KB/s

            // If the progress has changed, calculate the instantaneous throughput.
            if (lastProgress != progress) {
                lastProgress = progress;

                // Just like for the average throughput, this is calculated by diving number of
                // bytes sent since last progress change by the time since the change.
                final float bytesSentSinceProgressChanged = bytesSent - lastProgressChangeBytesSent;
                final float timeSinceProgressChanged = Math.max(MIN_INTERVAL, timestamp - lastProgressChangeTimestamp);
                instantaneousThroughput = bytesSentSinceProgressChanged / timeSinceProgressChanged; // bytes / ms = KB/s

                // And reset the counters.
                lastProgressChangeTimestamp = timestamp;
                lastProgressChangeBytesSent = bytesSent;

                progressLiveData.postValue(new ThroughputData(progress, instantaneousThroughput, averageThroughput));
            }
        }
        // When done, reset the counter.
        if (bytesSent == imageSize) {
            Timber.i("Image (%d bytes) sent in %d ms (avg speed: %f kB/s)",
                    imageSize - initialBytesSent,
                    timestamp - uploadStartTimestamp,
                    (float) (imageSize - initialBytesSent) / (float) (timestamp - uploadStartTimestamp)
            );
            // Reset the initial bytes counter, so if there is a next image uploaded afterwards,
            // it will start the throughput calculations again.
            initialBytesSent = NOT_STARTED;
        }
    }

    @Override
    public void onUpgradeCompleted() {
        stateLiveData.postValue(State.COMPLETE);
        Timber.i("Upgrade complete");
        setLoggingEnabled(true);
        postReady();
    }

    @Override
    public void onUpgradeCanceled(final FirmwareUpgradeManager.State state) {
        progressLiveData.postValue(null);
        stateLiveData.postValue(State.IDLE);
        cancelledEvent.post();
        Timber.w("Upgrade cancelled");
        setLoggingEnabled(true);
        postReady();
    }

    @Override
    public void onUpgradeFailed(final FirmwareUpgradeManager.State state, final McuMgrException error) {
        progressLiveData.postValue(null);
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
