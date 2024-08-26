/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.viewmodel.mcumgr;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.jetbrains.annotations.NotNull;

import java.net.URI;

import javax.inject.Inject;
import javax.inject.Named;

import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.ble.McuMgrBleTransport;
import io.runtime.mcumgr.dfu.FirmwareUpgradeCallback;
import io.runtime.mcumgr.dfu.FirmwareUpgradeController;
import io.runtime.mcumgr.dfu.FirmwareUpgradeSettings;
import io.runtime.mcumgr.dfu.mcuboot.FirmwareUpgradeManager;
import io.runtime.mcumgr.dfu.mcuboot.model.ImageSet;
import io.runtime.mcumgr.dfu.suit.SUITUpgradeManager;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.image.SUITImage;
import io.runtime.mcumgr.sample.observable.ConnectionParameters;
import io.runtime.mcumgr.sample.observable.ObservableMcuMgrBleTransport;
import io.runtime.mcumgr.sample.utils.ZipPackage;
import io.runtime.mcumgr.sample.viewmodel.SingleLiveEvent;
import no.nordicsemi.android.ble.ConnectionPriorityRequest;
import timber.log.Timber;

public class ImageUpgradeViewModel extends McuMgrViewModel {
    public enum State {
        IDLE,
        VALIDATING,
        UPLOADING,
        PAUSED,
        TESTING,
        CONFIRMING,
        PROCESSING,
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
        public float averageThroughput;

        public ThroughputData(final int progress, final float averageThroughput) {
            this.progress = progress;
            this.averageThroughput = averageThroughput;
        }
    }

    @Nullable
    private final McuMgrBleTransport bleTransport;
    @NonNull
    private final FirmwareUpgradeManager manager;
    @NonNull
    private final SUITUpgradeManager suitManager;
	private final Handler handler;
    private Runnable onSuitNotSupported;

    private final MutableLiveData<State> stateLiveData = new MutableLiveData<>();
    private final MutableLiveData<ThroughputData> progressLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> advancedSettingsExpanded = new MutableLiveData<>();
    private final MutableLiveData<McuMgrException> errorLiveData = new MutableLiveData<>();
    private final SingleLiveEvent<Void> cancelledEvent = new SingleLiveEvent<>();

    private long uploadStartTimestamp;
	private int imageSize, bytesSent, bytesSentSinceUploadStated, lastProgress;
    /** A value indicating that the upload has not been started before. */
    private final static int NOT_STARTED = -1;
	/** How often the throughput data should be sent to the graph. */
	private final static long REFRESH_RATE = 100L; /* ms */

    @Inject
    ImageUpgradeViewModel(@NonNull final McuMgrTransport transporter,
                          @NonNull final FirmwareUpgradeManager manager,
                          @NonNull final SUITUpgradeManager suitManager,
                          @NonNull final HandlerThread thread,
                          @NonNull @Named("busy") final MutableLiveData<Boolean> state) {
        super(state);
        if (transporter instanceof McuMgrBleTransport bleTransporter) {
            this.bleTransport = bleTransporter;
        } else {
            this.bleTransport = null;
        }
        this.manager = manager;
        this.manager.setFirmwareUpgradeCallback(new FirmwareUpgradeCallback<>() {

            @Override
            public void onUpgradeStarted(final FirmwareUpgradeController controller) {
                ImageUpgradeViewModel.this.onUpgradeStarted(State.VALIDATING);
            }

            @Override
            public void onStateChanged(
                    final FirmwareUpgradeManager.State prevState,
                    final FirmwareUpgradeManager.State newState
            ) {
                setLoggingEnabled(newState != FirmwareUpgradeManager.State.UPLOAD);
                switch (newState) {
                    case UPLOAD -> {
                        Timber.i("Uploading firmware...");
                        bytesSentSinceUploadStated = NOT_STARTED;
                        stateLiveData.postValue(State.UPLOADING);
                    }
                    case TEST -> {
                        handler.removeCallbacks(graphUpdater);
                        stateLiveData.postValue(State.TESTING);
                    }
                    case CONFIRM -> {
                        handler.removeCallbacks(graphUpdater);
                        stateLiveData.postValue(State.CONFIRMING);
                    }
                    case RESET -> stateLiveData.postValue(State.RESETTING);
                }
            }

            @Override
            public void onUploadProgressChanged(final int bytesSent, final int imageSize, final long timestamp) {
                ImageUpgradeViewModel.this.onUploadProgressChanged(bytesSent, imageSize, timestamp);
            }

            @Override
            public void onUpgradeCompleted() {
                ImageUpgradeViewModel.this.onUpgradeCompleted();
            }

            @Override
            public void onUpgradeCanceled(final FirmwareUpgradeManager.State state) {
                ImageUpgradeViewModel.this.onUpgradeCancelled();
            }

            @Override
            public void onUpgradeFailed(final FirmwareUpgradeManager.State state, final McuMgrException error) {
                ImageUpgradeViewModel.this.onUpgradeFailed(error);
            }
        });

        this.suitManager = suitManager;
        this.suitManager.setFirmwareUpgradeCallback(new FirmwareUpgradeCallback<>() {
            @Override
            public void onUpgradeStarted(FirmwareUpgradeController controller) {
                Timber.i("Upgrade started");
                ImageUpgradeViewModel.this.onUpgradeStarted(State.PROCESSING);
            }

            @Override
            public void onStateChanged(SUITUpgradeManager.State prevState, SUITUpgradeManager.State newState) {
                setLoggingEnabled(newState == SUITUpgradeManager.State.PROCESSING);
                switch (newState) {
                    case PROCESSING -> {
                        handler.removeCallbacks(graphUpdater);
                        stateLiveData.postValue(State.PROCESSING);
                    }
                    case UPLOADING_ENVELOPE -> {
                        Timber.i("Uploading envelope...");
                        bytesSentSinceUploadStated = NOT_STARTED;
                        stateLiveData.postValue(State.UPLOADING);
                    }
                    case UPLOADING_RESOURCE -> {
                        Timber.i("Uploading resource...");
                        bytesSentSinceUploadStated = NOT_STARTED;
                        stateLiveData.postValue(State.UPLOADING);
                    }
                }
            }

            @Override
            public void onUploadProgressChanged(final int bytesSent, final int imageSize, final long timestamp) {
                // Log the last part of the logs.
                if (bytesSent > imageSize - 510) {
                    setLoggingEnabled(true);
                }
                ImageUpgradeViewModel.this.onUploadProgressChanged(bytesSent, imageSize, timestamp);
            }

            @Override
            public void onUpgradeCompleted() {
                setLoggingEnabled(true);
                ImageUpgradeViewModel.this.onUpgradeCompleted();
            }

            @Override
            public void onUpgradeCanceled(final SUITUpgradeManager.State state) {
                ImageUpgradeViewModel.this.onUpgradeCancelled();
            }

            @Override
            public void onUpgradeFailed(final SUITUpgradeManager.State state, final McuMgrException error) {
                ImageUpgradeViewModel.this.onUpgradeFailed(error);
            }
        });
        this.handler = new Handler(thread.getLooper());

        stateLiveData.setValue(State.IDLE);
        progressLiveData.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        manager.setFirmwareUpgradeCallback(null);
        suitManager.setFirmwareUpgradeCallback(null);
        onSuitNotSupported = null;
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

    @Nullable
    public LiveData<ConnectionParameters> getConnectionParameters() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            bleTransport instanceof ObservableMcuMgrBleTransport ot) {
            return ot.getConnectionParameters();
        }
        return null;
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
        // If SUIT is not supported by the device, the image will be uploaded using Image Manager.
        onSuitNotSupported = () -> {
            onSuitNotSupported = null;
            ImageSet images;
            try {
                // The following supports Mcu Manager Image and a standalone SUIT envelope.
                images = new ImageSet().add(data);
            } catch (final Exception e) {
                try {
                    final ZipPackage zip = new ZipPackage(data);
                    final byte[] envelope = zip.getSuitEnvelope();
                    if (envelope != null) {
                        // SUIT envelope can also be sent using Image Manager.
                        // For example for device recovery.
                        // Usually, a single file wouldn't be placed in a ZIP file, but let's try.
                        images = new ImageSet().add(envelope);
                    } else {
                        images = zip.getBinaries();
                    }
                } catch (final Exception e1) {
                    Timber.e(e, "Invalid image file");
                    errorLiveData.setValue(new McuMgrException("Invalid image file."));
                    return;
                }
            }
            upgradeWithImageManager(images, mode, eraseSettings, estimatedSwapTime, windowCapacity, memoryAlignment);
        };
        // First, try using SUIT Manager.
        try {
            // Try parsing as SUIT Envelope.
            final SUITImage envelope = SUITImage.fromBytes(data);
            // Having a single ZIP file we can't provide additional resources.
            // HTTP(S) transport is currently not supported.
            suitManager.setResourceCallback(new SUITUpgradeManager.OnResourceRequiredCallback() {
                @Override
                public void onResourceRequired(@NotNull URI uri, SUITUpgradeManager.@NotNull ResourceCallback callback) {
                    callback.error(new UnsupportedOperationException("Resource required callback not supported."));
                }

                @Override
                public void onUploadCancelled() {
                    // Ignore
                }
            });
            upgradeWithSUITManager(envelope, windowCapacity, memoryAlignment);
        } catch (final Exception e) {
            try {
                // Try reading SUIT envelope from ZIP file.
                final ZipPackage zip = new ZipPackage(data);
                // A ZIP file may contain multiple files, but only one SUIT envelope.
                final byte[] envelope = zip.getSuitEnvelope();
                if (envelope != null) {
                    final SUITImage suitImage = SUITImage.fromBytes(envelope);
                    // During the upload, SUIT manager may request additional resources.
                    // This callback will return the requested resource from the ZIP file.
                    suitManager.setResourceCallback(new SUITUpgradeManager.OnResourceRequiredCallback() {
                        @Override
                        public void onResourceRequired(@NotNull final URI uri, @NotNull final SUITUpgradeManager.ResourceCallback callback) {
                            if (!uri.getScheme().equals("file")) {
                                callback.error(new McuMgrException("Cannot obtain " + uri + ". Only file:// scheme is supported."));
                                return;
                            }
                            final byte[] data = zip.getResource(uri.getSchemeSpecificPart().substring(2));
                            if (data != null) {
                                callback.provide(data);
                            } else {
                                callback.error(new McuMgrException(uri + " not found in ZIP file."));
                            }
                        }

                        @Override
                        public void onUploadCancelled() {
                            // Ignore
                        }
                    });
                    upgradeWithSUITManager(suitImage, windowCapacity, memoryAlignment);
                    return;
                }
                throw new NullPointerException();
            } catch (final Exception e2) {
                // Fallback to Image Manager.
                onSuitNotSupported.run();
            }
        }
    }

    private void upgradeWithImageManager(
            @NonNull final ImageSet images,
            @NonNull final FirmwareUpgradeManager.Mode mode,
            final boolean eraseSettings,
            final int estimatedSwapTime,
            final int windowCapacity,
            final int memoryAlignment
    ) {
        requestHighConnectionPriority();

        // Set the upgrade mode.
        manager.setMode(mode);

        final FirmwareUpgradeManager.Settings settings = new FirmwareUpgradeManager.Settings.Builder()
                .setEraseAppSettings(eraseSettings)
                // rF52840, due to how the flash memory works, requires ~20 sec to erase images.
                .setEstimatedSwapTime(estimatedSwapTime)
                // Set the window capacity. Values > 1 enable a new implementation for uploading
                // the images, which makes use of SMP pipelining feature.
                // The app will send this many packets immediately, without waiting for notification
                // confirming each packet. This value should be lower or equal to MCUMGR_TRANSPORT_NETBUF_COUNT - 1
                // (https://github.com/zephyrproject-rtos/zephyr/blob/19f645edd40b38e54f505135beced1919fdc7715/subsys/mgmt/mcumgr/transport/Kconfig#L32)
                // parameter in KConfig in NCS / Zephyr configuration and should also be supported
                // on Mynewt devices.
                // Mind, that in Zephyr,  before https://github.com/zephyrproject-rtos/zephyr/pull/41959
                // was merged, the device required data to be sent with memory alignment. Otherwise,
                // the device would ignore uneven bytes and reply with lower than expected offset
                // causing multiple packets to be sent again dropping the speed instead of increasing it.
                .setWindowCapacity(windowCapacity)
                // Set the selected memory alignment. In the app this defaults to 4 to match Nordic
                // devices, but can be modified in the UI.
                .setMemoryAlignment(memoryAlignment)
                .build();

        manager.start(images, settings);
    }

    private void upgradeWithSUITManager(
            @NonNull final SUITImage envelope,
            final int windowCapacity,
            final int memoryAlignment
    ) {
        requestHighConnectionPriority();

        final FirmwareUpgradeSettings settings = new FirmwareUpgradeSettings.Builder()
                .setWindowCapacity(windowCapacity)
                .setMemoryAlignment(memoryAlignment)
                .build();
        suitManager.start(settings, envelope.getData());
    }

    public void pause() {
        if (manager.isInProgress() || suitManager.isInProgress()) {
            handler.removeCallbacks(graphUpdater);
            stateLiveData.postValue(State.PAUSED);
            manager.pause();
            suitManager.pause();
            Timber.i("Upload paused");
            setLoggingEnabled(true);
            setReady();
        }
    }

    public void resume() {
        if (manager.isPaused() || suitManager.isPaused()) {
            setBusy();
            stateLiveData.postValue(State.UPLOADING);
            Timber.i("Upload resumed");
            bytesSentSinceUploadStated = NOT_STARTED;
            setLoggingEnabled(false);
            manager.resume();
            suitManager.resume();
        }
    }

    public void cancel() {
        manager.cancel();
        suitManager.cancel();
    }

    private final Runnable graphUpdater = new Runnable() {
        @Override
        public void run() {
            if (stateLiveData.getValue() != State.UPLOADING) {
                return;
            }

            final long timestamp = SystemClock.uptimeMillis();
            // Calculate the current upload progress.
            final int progress = (int) (bytesSent * 100.f /* % */ / imageSize);
            if (lastProgress != progress) {
                lastProgress = progress;

                // Calculate the average throughout.
                // This is done by diving number of bytes sent since upload has been started (or resumed)
                // by the time since that moment. The minimum time of MIN_INTERVAL ms prevents from
                // graph peaks that may happen when .
                final float bytesSentSinceUploadStarted = bytesSent - bytesSentSinceUploadStated;
                final float timeSinceUploadStarted = timestamp - uploadStartTimestamp;
                final float averageThroughput = bytesSentSinceUploadStarted / timeSinceUploadStarted; // bytes / ms = KB/s

                progressLiveData.postValue(new ThroughputData(progress, averageThroughput));
            }

            if (stateLiveData.getValue() == State.UPLOADING) {
                handler.postAtTime(this, timestamp + REFRESH_RATE);
            }
        }
    };

    private void onUpgradeStarted(State state) {
        postBusy();
        progressLiveData.setValue(null);
        stateLiveData.setValue(state);
    }

    private void onUploadProgressChanged(final int bytesSent, final int imageSize, final long timestamp) {
        ImageUpgradeViewModel.this.imageSize = imageSize;
        ImageUpgradeViewModel.this.bytesSent = bytesSent;

        final long uptimeMillis = SystemClock.uptimeMillis();

        // Check if this is the first time this method is called since:
        // - the start of an upload
        // - after resume
        if (bytesSentSinceUploadStated == NOT_STARTED) {
            lastProgress = NOT_STARTED;

            // If a new image started being sending, clear the progress graph.
            progressLiveData.postValue(null);

            // To calculate the throughput it is necessary to store the initial timestamp and
            // the number of bytes sent so far. Mind, that the upload may be resumed from any point,
            // not necessarily from the beginning.
            uploadStartTimestamp = uptimeMillis;
            bytesSentSinceUploadStated = bytesSent;

            // Begin updating the graph.
            handler.removeCallbacks(graphUpdater);
            handler.postAtTime(graphUpdater, uptimeMillis + REFRESH_RATE);
        }
        // When done, reset the counter.
        if (bytesSent == imageSize) {
            Timber.i("Image (%d bytes) sent in %d ms (avg speed: %f kB/s)",
                    imageSize - bytesSentSinceUploadStated,
                    uptimeMillis - uploadStartTimestamp,
                    (float) (imageSize - bytesSentSinceUploadStated) / (float) (uptimeMillis - uploadStartTimestamp)
            );
            // Finish the graph.
            graphUpdater.run();
            // Reset the initial bytes counter, so if there is a next image uploaded afterwards,
            // it will start the throughput calculations again.
            bytesSentSinceUploadStated = NOT_STARTED;
        }
    }

    private void onUpgradeCompleted() {
        stateLiveData.postValue(State.COMPLETE);
        onSuitNotSupported = null;
        Timber.i("Upgrade complete");
        setLoggingEnabled(true);
        postReady();
    }

    private void onUpgradeCancelled() {
        handler.removeCallbacks(graphUpdater);
        progressLiveData.postValue(null);
        stateLiveData.postValue(State.IDLE);
        cancelledEvent.post();
        onSuitNotSupported = null;
        Timber.w("Upgrade cancelled");
        setLoggingEnabled(true);
        postReady();
    }

    private void onUpgradeFailed(final McuMgrException error) {
        if (onSuitNotSupported != null &&
                error instanceof McuMgrErrorException ee &&
                ee.getCode() == McuMgrErrorCode.NOT_SUPPORTED) {
            suitManager.setResourceCallback(null);
            onSuitNotSupported.run();
            return;
        }
        handler.removeCallbacks(graphUpdater);
        errorLiveData.postValue(error);
        onSuitNotSupported = null;
        setLoggingEnabled(true);
        Timber.e(error, "Upgrade failed");
        postReady();
    }

    private void requestHighConnectionPriority() {
        if (bleTransport != null) {
            bleTransport.requestConnPriority(ConnectionPriorityRequest.CONNECTION_PRIORITY_HIGH);
        }
    }

    private void setLoggingEnabled(final boolean enabled) {
        if (bleTransport != null) {
            bleTransport.setLoggingEnabled(enabled);
        }
    }
}
