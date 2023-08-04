package io.runtime.mcumgr.dfu;

import android.os.Handler;
import android.os.Looper;
import android.util.Pair;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executor;

import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.dfu.model.McuMgrImageSet;
import io.runtime.mcumgr.dfu.model.McuMgrTargetImage;
import io.runtime.mcumgr.exception.McuMgrException;

// TODO Add retries for each step

/**
 * Manages a McuManager firmware upgrade. Once initialized, <b>this object can only perform a single
 * firmware upgrade for a single device to completion</b>. In other words, the same
 * FirmwareUpgradeManager may start the upload, cancel the upload, and call start once again to
 * restart the firmware upgrade. However, once the upload is completed, the same
 * FirmwareUpgradeManager cannot not be used for subsequent firmware upgrades.
 * <p>
 * Like other MCU managers, FirmwareUpgradeManagers must be initialized with a
 * {@link McuMgrTransport} which defines the scheme and implements the transport methods for
 * communicating with the device. FirmwareUpgradeManagers additionally require a callback and
 * the image data to upload.
 * <p>
 * Once initialized, a firmware upgrade is started by calling {@link FirmwareUpgradeManager#start},
 * and can be paused, resumed, and canceled using {@link FirmwareUpgradeManager#pause},
 * {@link FirmwareUpgradeManager#resume}, and {@link FirmwareUpgradeManager#cancel}.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class FirmwareUpgradeManager implements FirmwareUpgradeController {

    private final static Logger LOG = LoggerFactory.getLogger(FirmwareUpgradeManager.class);

    //******************************************************************
    // Firmware Upgrade State
    //******************************************************************

    public enum State {
        NONE, VALIDATE, UPLOAD, TEST, RESET, CONFIRM;

        public boolean isInProgress() {
            return this != NONE;
        }
    }

    //******************************************************************
    // Firmware Upgrade Mode
    //******************************************************************

    public enum Mode {
        /**
         * When this mode is set, the manager will send the test and reset commands to
         * the device after the upload is complete. The device will reboot and will run the new
         * image on its next boot. If the new image supports auto-confirm feature, it will try to
         * confirm itself and change state to permanent. If not, test image will run just once
         * and will be swapped again with the original image on the next boot.
         * <p>
         * Use this mode if you just want to test the image, when it can confirm itself.
         */
        TEST_ONLY,
        /**
         * When this flag is set, the manager will send confirm and reset commands immediately
         * after upload.
         * <p>
         * Use this mode if when the new image does not support both auto-confirm feature and
         * SMP service and could not be confirmed otherwise.
         */
        CONFIRM_ONLY,
        /**
         * When this flag is set, the manager will first send test followed by reset commands,
         * then it will reconnect to the new application and will send confirm command.
         * <p>
         * Use this mode when the new image supports SMP service and you want to test it
         * before confirming.
         */
        TEST_AND_CONFIRM
    }

    //******************************************************************
    // Firmware Upgrade Mode
    //******************************************************************

    public static class Settings {
        @NotNull
        public final McuMgrTransport transport;

        /**
         * Estimated time required for swapping images, in milliseconds.
         * If the mode is set to {@link FirmwareUpgradeManager.Mode#TEST_AND_CONFIRM}, the manager will try to reconnect after
         * this time. 0 by default.
         */
        public final int estimatedSwapTime;

        /**
         * The upload window capacity for faster image uploads. A capacity greater than 1 will enable
         * using the faster window upload implementation.
         */
        public final int windowCapacity;

        /**
         * Memory alignment. Value 1 disables memory alignment.
         */
        public final int memoryAlignment;

        private Settings(@NotNull final McuMgrTransport transport,
                         final int estimatedSwapTime,
                         final int windowCapacity,
                         final int memoryAlignment) {
            this.transport = transport;
            this.estimatedSwapTime = estimatedSwapTime;
            this.windowCapacity = windowCapacity;
            this.memoryAlignment = memoryAlignment;
        }

        public static class Builder {
            @NotNull
            private final McuMgrTransport transport;
            private int estimatedSwapTime = 0;
            private int windowCapacity = 1;
            private int memoryAlignment = 1;

            public Builder(@NotNull final McuMgrTransport transport) {
                this.transport = transport;
            }

            /**
             * Sets estimated time for the image swap. The device should reset after that time after
             * successful DFU operation.
             * <p>
             * If the device requires long time for the images to be swapped, and this is not set,
             * the reconnection may fail and DFU process may be reported as unsuccessful despite
             * it actually working correctly. This is important with
             * {@link FirmwareUpgradeManager.Mode#TEST_AND_CONFIRM} mode, which
             * reconnects to the device after it resets.
             * @param time the swap time in milliseconds.
             * @return The builder.
             */
            public Builder setEstimatedSwapTime(final int time) {
                this.estimatedSwapTime = Math.max(0, time);
                return this;
            }

            /**
             * Sets window capacity. On Zephyr this is equal to MCUMGR_BUF_COUNT value, which defaults to 4.
             * @param windowCapacity number of windows that can be sent in parallel.
             * @return The builder.
             */
            public Builder setWindowCapacity(final int windowCapacity) {
                this.windowCapacity = Math.max(0, windowCapacity);
                return this;
            }

            /**
             * The memory alignment value should match the device's memory layout.
             * Some devices require the chunks to be word or 16-byte aligned to be saved.
             * <p>
             * Value 1 disables alignment and chunks will be sent as big as possible.
             * @param alignment device memory alignment.
             * @return The builder.
             */
            public Builder setMemoryAlignment(final int alignment) {
                this.memoryAlignment = Math.max(1, alignment);
                return this;
            }

            /**
             * Builds the settings object.
             * @return Settings.
             */
            public Settings build() {
                return new Settings(transport, estimatedSwapTime, windowCapacity, memoryAlignment);
            }
        }
    }

    //******************************************************************
    // Properties
    //******************************************************************

    @NotNull
    private final FirmwareUpgradePerformer mPerformer;

    @NotNull
    private final McuMgrTransport mTransport;

    /**
     * Firmware upgrade callback passed into the constructor or set before the upload has started.
     */
    private FirmwareUpgradeCallback mCallback;

    /**
     * The manager mode. By default the {@link Mode#TEST_AND_CONFIRM} mode is set.
     */
    private Mode mMode = Mode.TEST_AND_CONFIRM;

    /**
     * Flag for setting callbacks to run on the main UI thread.
     */
    private boolean mUiThreadCallbacks = true;

    /**
     * Estimated time required for swapping images, in milliseconds.
     * If the mode is set to {@link Mode#TEST_AND_CONFIRM}, the manager will try to reconnect after
     * this time. 0 by default.
     */
    private int mEstimatedSwapTime = 0;

    /**
     * The upload window capacity for faster image uploads. A capacity greater than 1 will enable
     * using the faster window upload implementation.
     */
    private int mWindowCapacity = 1;

    /**
     * The memory alignment of a device. This value is used to trim each packet of data sent.
     * By default, memory alignment is disabled (value = 1) and should be set to 4, 8, 16, or any
     * other value that the flash is aligned to. For Nordic devices this is equal to 4.
     */
    private int mMemoryAlignment = 1; // initially disabled for backwards compatibility.

    //******************************************************************
    // Firmware Upgrade Manager API
    //******************************************************************

    /**
     * Construct a firmware upgrade manager. If using this constructor, the callback must be set
     * using {@link #setFirmwareUpgradeCallback(FirmwareUpgradeCallback)} before calling
     * {@link FirmwareUpgradeManager#start}.
     *
     * @param transport the transporter to use.
     */
    public FirmwareUpgradeManager(@NotNull final McuMgrTransport transport) {
        this(transport, null);
    }

    /**
     * Construct a firmware upgrade manager.
     *
     * @param transport the transporter to use.
     * @param callback  the callback.
     */
    public FirmwareUpgradeManager(@NotNull final McuMgrTransport transport,
                                  @Nullable final FirmwareUpgradeCallback callback) {
        mTransport = transport;
        mCallback = callback;
        mPerformer = new FirmwareUpgradePerformer(mInternalCallback);
    }

    /**
     * Construct a firmware upgrade manager.
     *
     * @param settings the upgrade settings.
     * @param callback the callback.
     */
    public FirmwareUpgradeManager(@NotNull final Settings settings,
                                  @Nullable final FirmwareUpgradeCallback callback) {
        mTransport = settings.transport;
        mCallback = callback;
        mEstimatedSwapTime = settings.estimatedSwapTime;
        mWindowCapacity = settings.windowCapacity;
        mPerformer = new FirmwareUpgradePerformer(mInternalCallback);
    }

    /**
     * Get the transporter.
     *
     * @return Transporter for this new manager instance.
     */
    @NotNull
    public McuMgrTransport getTransporter() {
        return mTransport;
    }

    /**
     * Get the current {@link State} of the firmware upgrade.
     *
     * @return The current state.
     */
    public State getState() {
        return mPerformer.getState();
    }

    /**
     * If true, run all callbacks on the UI thread (default).
     *
     * @param uiThreadCallbacks true if all callbacks should run on the UI thread.
     */
    public void setCallbackOnUiThread(final boolean uiThreadCallbacks) {
        mUiThreadCallbacks = uiThreadCallbacks;
    }

    /**
     * Sets the manager callback.
     *
     * @param callback the callback for receiving status change events.
     */
    public void setFirmwareUpgradeCallback(@Nullable final FirmwareUpgradeCallback callback) {
        mCallback = callback;
    }

    /**
     * Sets the manager mode. By default the {@link Mode#TEST_AND_CONFIRM} mode is used.
     * The mode may be set only before calling {@link #start(byte[])} method.
     *
     * @param mode the manager mode.
     * @see Mode#TEST_ONLY TEST_ONLY
     * @see Mode#CONFIRM_ONLY CONFIRM_ONLY
     * @see Mode#TEST_AND_CONFIRM TEST_AND_CONFIRM
     */
    public void setMode(@NotNull final Mode mode) {
        if (mPerformer.isBusy()) {
            LOG.info("Firmware upgrade is already in progress");
            return;
        }
        mMode = mode;
    }

    /**
     * Sets the estimated time required to swap images after uploading the image successfully.
     * If the mode was set to {@link Mode#TEST_AND_CONFIRM}, the manager will wait this long
     * before trying to reconnect to the device.
     *
     * @param swapTime estimated time required for swapping images, in milliseconds. 0 by default.
     */
    public void setEstimatedSwapTime(final int swapTime) {
        if (mPerformer.isBusy()) {
            LOG.info("Firmware upgrade is already in progress");
            return;
        }
        mEstimatedSwapTime = Math.max(swapTime, 0);
    }

    /**
     * A window capacity > 1 enables a faster image upload implementation (SMP pipelining) which
     * allows {@code windowCapacity} concurrent upload requests. The manager will send multiple
     * packets, trimmed to match the memory alignment. It will then wait for corresponding
     * notifications and continue to send until the complete image is sent.
     * <p>
     * This value should match MCUMGR_BUF_COUNT - 1
     * (https://github.com/zephyrproject-rtos/zephyr/blob/bd4ddec0c8c822bbdd420bd558b62c1d1a532c16/subsys/mgmt/mcumgr/Kconfig#L550)
     * in Zephyr KConfig, which is by default set to 4. One buffer is used for sending responses.
     * <p>
     * Mind, that the speed increases only if the returned offsets match the required offset
     * (initial offset + sent packet size). In other case the packets with unexpected offsets
     * are dropped by the device causing teh packets to be resent, which actually makes the upload
     * slower.
     *
     * Pause and resume will throw an exception when window upload is used.
     * @param windowCapacity the maximum number of concurrent upload requests at any time.
     */
    public void setWindowUploadCapacity(final int windowCapacity) {
        if (windowCapacity <= 0) {
            throw new IllegalArgumentException("window capacity must be > 0");
        }
        if (mPerformer.isBusy()) {
            LOG.info("Firmware upgrade is already in progress");
            return;
        }
        mWindowCapacity = windowCapacity;
    }

    /**
     * Sets the memory alignment of a device. This value is used to trim each packet of data sent.
     * <p>
     * This a specially helps when used with pipelining (window capacity > 1), where
     * the mobile device sends multiple packets without waiting for a response from a device.
     * If the remote device can store only the number of bytes divisible by its memory alignment,
     * e.g. a word (4 bytes), it would ignore uneven bytes and reply with a lower than expected
     * offset, also ignoring already sent packets following the first one. All packets except the
     * first one would have to be sent again, not with slightly decremented offset.
     * By trimming to memory alignment, this library makes sure that all bytes sent are consumed.
     * <p>
     * With https://github.com/zephyrproject-rtos/zephyr/pull/41959 PR merged, you can set the
     * alignment to 1 (disabled), as the flash manager itself takes care of alignment.
     */
    public void setMemoryAlignment(final int alignment) {
        if (alignment < 1) {
            throw new IllegalArgumentException("memory alignment must be >= 1");
        }
        if (mPerformer.isBusy()) {
            LOG.info("Firmware upgrade is already in progress");
            return;
        }
        mMemoryAlignment = alignment;
    }

    /**
     * Start the upgrade.
     * <p>
     * The specified image file will be sent to the target using the
     * given transport, then depending on the mode, verified using "test" command or confirmed
     * using "confirm" command. If successful, the reset command will be sent. The device should
     * boot with the new firmware. The manager will try to connect to the SMP server on the new
     * firmware and confirm the new images if they were not confirmed before, or they didn't confirm
     * automatically.
     */
    public synchronized void start(final byte @NotNull [] imageData) throws McuMgrException {
        start(new McuMgrImageSet().add(imageData), false);
    }

    /**
     * Start the upgrade for multi-core devices. Each image is paired with image partition identifier.
     * E.g. image 0 is the main or application core. Image 1 is the next core, e.g network core, etc.
     * <p>
     * The specified image files will be sent to the target to image partitions identified
     * by the Integer parameter paired with each image using the transport specified for the manager,
     * then, depending on the mode, verified using "test" command or confirmed using "confirm"
     * command. If successful, the reset command will be sent. The device should boot with the
     * new firmware. The manager will try to connect to the SMP server on the new firmware and
     * confirm the new images if they were not confirmed before, or they didn't confirm
     * automatically.
     *
     * @param images       list of images with image index.
     * @param eraseStorage should the app settings be erased, or not (default true).
     * @deprecated Use {@link #start(McuMgrImageSet, boolean)} instead.
     */
    @Deprecated(since = "1.8")
    public synchronized void start(@NotNull final List<Pair<Integer, byte[]>> images,
                                   final boolean eraseStorage) throws McuMgrException {
        start(new McuMgrImageSet().add(images), eraseStorage);
    }

    /**
     * Starts an upgrade with given image set. The targets define the image index and
     * slot to which the binary should be sent.
     * <p>
     * This method can be used for mutli-core devices (each core is identified by
     * {@link McuMgrTargetImage#imageIndex}) with and without Direct XIP feature.
     * <p>
     * Direct XIP is a feature added in NCS 2.5 allowing to boot a device from a non-primary slot.
     * For such devices the correct image has to be sent (compiled for that specific slot), depending
     * on the slot number with the active image. This feature removes the need for a swap after an
     * update, as the newly uploaded image is already in the correct slot.
     *
     * @param images       set of images. For direct XIP this set should contain images compiled
     *                     for both slots. The correct image will be chosen automatically.
     * @param eraseStorage should the app settings be erased, or not (default true).
     * @since 1.8
     */
    public synchronized void start(@NotNull final McuMgrImageSet images,
                                   final boolean eraseStorage) throws McuMgrException {
        if (mPerformer.isBusy()) {
            LOG.info("Firmware upgrade is already in progress");
            return;
        }

        // Start upgrade.
        mInternalCallback.onUpgradeStarted(this);
        final Settings settings = new Settings.Builder(mTransport)
                .setEstimatedSwapTime(mEstimatedSwapTime)
                .setWindowCapacity(mWindowCapacity)
                .setMemoryAlignment(mMemoryAlignment)
                .build();
        mPerformer.start(settings, mMode, images, eraseStorage);
    }

    //******************************************************************
    // Upload Controller
    //******************************************************************

    @Override
    public synchronized void cancel() {
        mPerformer.cancel();
    }

    @Override
    public synchronized void pause() {
        mPerformer.pause();
    }

    @Override
    public synchronized void resume() {
        mPerformer.resume();
    }

    @Override
    public synchronized boolean isPaused() {
        return mPerformer.isPaused();
    }

    @Override
    public synchronized boolean isInProgress() {
        return mPerformer.isBusy() && !isPaused();
    }

    //******************************************************************
    // Internal Callback forwarder
    //******************************************************************

    /**
     * Internal callback to route callbacks to the UI thread if the flag has been set.
     */
    private final FirmwareUpgradeCallback mInternalCallback = new FirmwareUpgradeCallback() {
        private MainThreadExecutor mMainThreadExecutor;

        private MainThreadExecutor getMainThreadExecutor() {
            if (mMainThreadExecutor == null) {
                mMainThreadExecutor = new MainThreadExecutor();
            }
            return mMainThreadExecutor;
        }

        @Override
        public void onUpgradeStarted(final FirmwareUpgradeController controller) {
            if (mCallback == null) {
                return;
            }
            if (mUiThreadCallbacks) {
                getMainThreadExecutor().execute(() -> mCallback.onUpgradeStarted(controller));
            } else {
                mCallback.onUpgradeStarted(controller);
            }
        }

        @Override
        public void onStateChanged(final State prevState, final State newState) {
            if (mCallback == null) {
                return;
            }
            if (mUiThreadCallbacks) {
                getMainThreadExecutor().execute(() -> mCallback.onStateChanged(prevState, newState));
            } else {
                mCallback.onStateChanged(prevState, newState);
            }
        }

        @Override
        public void onUpgradeCompleted() {
            if (mCallback == null) {
                return;
            }
            if (mUiThreadCallbacks) {
                getMainThreadExecutor().execute(() -> mCallback.onUpgradeCompleted());
            } else {
                mCallback.onUpgradeCompleted();
            }
        }

        @Override
        public void onUpgradeFailed(final State state, final McuMgrException error) {
            if (mCallback == null) {
                return;
            }
            if (mUiThreadCallbacks) {
                getMainThreadExecutor().execute(() -> mCallback.onUpgradeFailed(state, error));
            } else {
                mCallback.onUpgradeFailed(state, error);
            }
        }

        @Override
        public void onUpgradeCanceled(final State state) {
            if (mCallback == null) {
                return;
            }
            if (mUiThreadCallbacks) {
                getMainThreadExecutor().execute(() -> mCallback.onUpgradeCanceled(state));
            } else {
                mCallback.onUpgradeCanceled(state);
            }
        }

        @Override
        public void onUploadProgressChanged(final int bytesSent, final int imageSize, final long timestamp) {
            if (mCallback == null) {
                return;
            }
            if (mUiThreadCallbacks) {
                getMainThreadExecutor().execute(() -> mCallback.onUploadProgressChanged(bytesSent, imageSize, timestamp));
            } else {
                mCallback.onUploadProgressChanged(bytesSent, imageSize, timestamp);
            }
        }
    };

    //******************************************************************
    // Main Thread Executor
    //******************************************************************

    /**
     * Used to execute callbacks on the main UI thread.
     */
    private static class MainThreadExecutor implements Executor {
        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NotNull Runnable command) {
            mainThreadHandler.post(command);
        }
    }
}
