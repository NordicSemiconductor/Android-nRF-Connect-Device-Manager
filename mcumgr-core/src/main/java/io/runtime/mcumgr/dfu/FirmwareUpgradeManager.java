package io.runtime.mcumgr.dfu;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.Executor;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrScheme;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.exception.McuMgrTimeoutException;
import io.runtime.mcumgr.image.McuMgrImage;
import io.runtime.mcumgr.managers.DefaultManager;
import io.runtime.mcumgr.managers.ImageManager;
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.response.img.McuMgrImageStateResponse;
import io.runtime.mcumgr.transfer.TransferController;
import io.runtime.mcumgr.transfer.UploadCallback;

import static io.runtime.mcumgr.transfer.ImageUploaderKt.windowUpload;

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

    /**
     * Performs the image upload, test, and confirmation steps.
     */
    private final ImageManager mImageManager;

    /**
     * Performs the reset command.
     */
    private final DefaultManager mDefaultManager;

    /**
     * Upload controller used to pause, resume, and cancel upload. Set when the upload is started.
     */
    private TransferController mUploadController;

    /**
     * Firmware upgrade callback passed into the constructor or set before the upload has started.
     */
    private FirmwareUpgradeCallback mCallback;

    /**
     * Image data to upload.
     */
    private byte[] mImageData;

    /**
     * Hash of the image data.
     */
    private byte[] mHash;

    /**
     * The manager mode. By default the {@link Mode#TEST_AND_CONFIRM} mode is set.
     */
    private Mode mMode = Mode.TEST_AND_CONFIRM;

    /**
     * State of the firmware upgrade.
     */
    private State mState;

    /**
     * Paused flag.
     */
    private boolean mPaused = false;

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
     * The timestamp at which the response to Reset command was received.
     * Assuming that the target device has reset just after sending this response,
     * the time difference between this moment and receiving disconnection event may be deducted
     * from the {@link #mEstimatedSwapTime}.
     */
    private long mResetResponseTime;

    /**
     * The upload window capacity for faster image uploads. A capacity greater than 1 will enable
     * using the faster window upload implementation.
     */
    private int mWindowCapacity = 1;

    /**
     * Construct a firmware upgrade manager. If using this constructor, the callback must be set
     * using {@link #setFirmwareUpgradeCallback(FirmwareUpgradeCallback)} before calling
     * {@link FirmwareUpgradeManager#start}.
     *
     * @param transport the transporter to use.
     */
    public FirmwareUpgradeManager(@NotNull McuMgrTransport transport) {
        this(transport, null);
    }

    /**
     * Construct a firmware upgrade manager.
     *
     * @param transport the transporter to use.
     * @param callback  the callback.
     */
    public FirmwareUpgradeManager(@NotNull McuMgrTransport transport,
                                  @Nullable FirmwareUpgradeCallback callback) {
        mState = State.NONE;
        mImageManager = new ImageManager(transport);
        mDefaultManager = new DefaultManager(transport);
        mCallback = callback;
    }

    /**
     * Get the transporter.
     *
     * @return Transporter for this new manager instance.
     */
    @NotNull
    public McuMgrTransport getTransporter() {
        return mImageManager.getTransporter();
    }

    /**
     * Get the transporter's scheme.
     *
     * @return The transporter's scheme.
     */
    @NotNull
    public McuMgrScheme getScheme() {
        return mImageManager.getScheme();
    }

    /**
     * Returns the upload MTU. MTU must be between 20 and 1024.
     *
     * @return The MTY.
     */
    public synchronized int getMtu() {
        return mImageManager.getMtu();
    }

    /**
     * If true, run all callbacks on the UI thread (default).
     *
     * @param uiThreadCallbacks true if all callbacks should run on the UI thread.
     */
    public void setCallbackOnUiThread(boolean uiThreadCallbacks) {
        mUiThreadCallbacks = uiThreadCallbacks;
    }

    /**
     * Sets the manager callback.
     *
     * @param callback the callback for receiving status change events.
     */
    public void setFirmwareUpgradeCallback(@Nullable FirmwareUpgradeCallback callback) {
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
    public void setMode(@NotNull Mode mode) {
        if (mState != State.NONE) {
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
    public void setEstimatedSwapTime(int swapTime) {
        mEstimatedSwapTime = Math.max(swapTime, 0);
    }

    /**
     * Set the MTU of the image upload.
     *
     * @param mtu the mtu (Maximum Transfer Unit).
     */
    public void setUploadMtu(int mtu) {
        mImageManager.setUploadMtu(mtu);
    }

    /**
     * A window capacity > 1 enables a faster image upload implementation which allows
     * {@code windowCapacity} concurrent upload requests.
     *
     * @param windowCapacity the maximum number of concurrent upload requests at any time.
     */
    public void setWindowUploadCapacity(int windowCapacity) {
        if (windowCapacity <= 0) {
            throw new IllegalArgumentException("window capacity must be > 0");
        }
        mWindowCapacity = windowCapacity;
    }

    /**
     * Start the upgrade.
     * <p>
     * The specified image file will be sent to the target using the
     * given transport, then verified using test command. If test successful, the reset
     * command will be sent. The device should boot with the new firmware.
     * The manager will try to connect to the SMP server on the new firmware and confirm
     * the upload.
     */
    public synchronized void start(byte @NotNull [] imageData) throws McuMgrException {
        if (mState != State.NONE) {
            LOG.info("Firmware upgrade is already in progress");
            return;
        }
        // Set image and validate
        mImageData = imageData;
        mHash = McuMgrImage.getHash(imageData);

        // Begin the upload
        mInternalCallback.onUpgradeStarted(this);
        validate();
    }

    //******************************************************************
    // Upload Controller
    //******************************************************************

    @Override
    public synchronized void cancel() {
        if (mState == State.VALIDATE) {
            mState = State.NONE;
            mPaused = false;
        } else if (mState == State.UPLOAD) {
            mUploadController.cancel();
            mPaused = false;
        }
    }

    @Override
    public synchronized void pause() {
        if (mState.isInProgress()) {
            mPaused = true;
            if (mState == State.UPLOAD) {
                mUploadController.pause();
            }
        }
    }

    @Override
    public synchronized void resume() {
        if (mPaused) {
            mPaused = false;
            currentState();
        }
    }

    @Override
    public synchronized boolean isPaused() {
        return mPaused;
    }

    @Override
    public synchronized boolean isInProgress() {
        return mState.isInProgress() && !isPaused();
    }

    //******************************************************************
    // Implementation
    //******************************************************************

    private synchronized void setState(State newState) {
        State prevState = mState;
        mState = newState;
        if (newState != prevState) {
            LOG.trace("Moving from state {} to state {}", prevState.name(), newState.name());
            mInternalCallback.onStateChanged(prevState, newState);
        }
    }

    private synchronized void validate() {
        setState(State.VALIDATE);
        if (!mPaused) {
            mImageManager.list(mImageValidateCallback);
        }
    }

    private synchronized void upload() {
        setState(State.UPLOAD);
        if (!mPaused) {
            if (mWindowCapacity > 1) {
                mUploadController = windowUpload(mImageManager, mImageData, mWindowCapacity,
                        mImageUploadCallback);
            } else {
                mUploadController = mImageManager.imageUpload(mImageData, mImageUploadCallback);
            }
        }
    }

    private synchronized void test() {
        setState(State.TEST);
        if (!mPaused) {
            mImageManager.test(mHash, mTestCallback);
        }
    }

    private synchronized void confirm() {
        setState(State.CONFIRM);
        if (!mPaused) {
            mImageManager.confirm(mHash, mConfirmCallback);
        }
    }

    private synchronized void verify() {
        setState(State.CONFIRM);
        if (!mPaused) {
            mImageManager.confirm(null, mConfirmCallback);
        }
    }

    private synchronized void reset() {
        setState(State.RESET);
        if (!mPaused) {
            mDefaultManager.getTransporter().addObserver(mResetObserver);
            mDefaultManager.reset(mResetCallback);
        }
    }

    private synchronized void success() {
        mState = State.NONE;
        mPaused = false;
        mInternalCallback.onUpgradeCompleted();
    }

    private synchronized void fail(McuMgrException error) {
        State failedState = mState;
        mState = State.NONE;
        mPaused = false;
        mInternalCallback.onUpgradeFailed(failedState, error);
    }

    private synchronized void cancelled(State state) {
        LOG.trace("Upgrade cancelled");
        mState = State.NONE;
        mPaused = false;
        mInternalCallback.onUpgradeCanceled(state);
    }

    //******************************************************************
    // McuManagerCallbacks
    //******************************************************************

    /**
     * State: VALIDATE.
     * Callback for the list command.
     */
    private final McuMgrCallback<McuMgrImageStateResponse> mImageValidateCallback = new McuMgrCallback<McuMgrImageStateResponse>() {
        @Override
        public void onResponse(@NotNull final McuMgrImageStateResponse response) {
            LOG.trace("Validation response: {}", response.toString());

            // Check for an error return code
            if (!response.isSuccess()) {
                fail(new McuMgrErrorException(response.getReturnCode()));
                return;
            }

            if (mState == State.NONE) {
                cancelled(State.VALIDATE);
                return;
            }

            McuMgrImageStateResponse.ImageSlot[] images = response.images;
            if (images == null) {
                LOG.error("Missing images information: {}", response.toString());
                fail(new McuMgrException("Missing images information"));
                return;
            }

            // Check if the new firmware is different than the active one.
            if (images.length > 0 && Arrays.equals(mHash, images[0].hash)) {
                if (images[0].confirmed) {
                    // The new firmware is already active and confirmed.
                    // No need to do anything.
                    success();
                } else {
                    // The new firmware is in test mode.
                    switch (mMode) {
                        case CONFIRM_ONLY:
                        case TEST_AND_CONFIRM:
                            // We have to confirm it.
                            confirm();
                            break;
                        case TEST_ONLY:
                            // Nothing to be done.
                            success();
                            break;
                    }
                }
                return;
            }

            // If the image in slot 1 is confirmed, we wont be able to erase or upload the image.
            // Therefore we must confirm the image in slot 0 and revalidate the image state.
            if (images.length > 1 && images[1].confirmed) {
                mImageManager.confirm(images[0].hash, new McuMgrCallback<McuMgrImageStateResponse>() {
                    @Override
                    public void onResponse(@NotNull McuMgrImageStateResponse response) {
                        if (!response.isSuccess()) {
                            fail(new McuMgrErrorException(response.getReturnCode()));
                            return;
                        }
                        validate();
                    }

                    @Override
                    public void onError(@NotNull McuMgrException error) {
                        fail(error);
                    }
                });
                return;
            }

            // If the image in slot 1 is pending, we won't be able to erase, upload or test the
            // image. Therefore, We must reset the device and revalidate the new image state.
            if (images.length > 1 && images[1].pending) {
                // Send reset command without changing state.
                mDefaultManager.getTransporter().addObserver(mResetObserver);
                mDefaultManager.reset(mResetCallback);
                return;
            }

            // Check if the new firmware was already sent.
            if (images.length > 1 && Arrays.equals(mHash, images[1].hash)) {
                // Firmware is identical to one on slot 1. No need to send anything.

                // If the test or confirm commands were not sent, proceed with next state.
                if (!images[1].pending) {
                    switch (mMode) {
                        case TEST_AND_CONFIRM:
                        case TEST_ONLY:
                            test();
                            break;
                        case CONFIRM_ONLY:
                            confirm();
                            break;
                    }
                    return;
                }

                // If image was already confirmed, reset (if confirm was planned), or fail.
                if (images[1].permanent) {
                    switch (mMode) {
                        case CONFIRM_ONLY:
                        case TEST_AND_CONFIRM:
                            // If confirm command was sent, just reset.
                            reset();
                            break;
                        case TEST_ONLY:
                            fail(new McuMgrException("Image already confirmed. Can't be tested."));
                            break;
                    }
                    return;
                }

                // If image was not confirmed, but test command was sent, confirm or reset.
                switch (mMode) {
                    case CONFIRM_ONLY:
                        confirm();
                        break;
                    case TEST_AND_CONFIRM:
                    case TEST_ONLY:
                        reset();
                        break;
                }
                return;
            }

            // Validation successful, begin image upload.
            upload();
        }

        @Override
        public void onError(@NotNull McuMgrException e) {
            fail(e);
        }
    };

    /**
     * State: TEST.
     * Callback for the test command.
     */
    private final McuMgrCallback<McuMgrImageStateResponse> mTestCallback = new McuMgrCallback<McuMgrImageStateResponse>() {
        @Override
        public void onResponse(@NotNull McuMgrImageStateResponse response) {
            LOG.trace("Test response: {}", response.toString());
            // Check for an error return code
            if (!response.isSuccess()) {
                fail(new McuMgrErrorException(response.getReturnCode()));
                return;
            }
            if (response.images.length != 2) {
                fail(new McuMgrException("Test response does not contain enough info"));
                return;
            }
            if (!response.images[1].pending) {
                fail(new McuMgrException("Tested image is not in a pending state."));
                return;
            }
            // Test image success, begin device reset.
            reset();
        }

        @Override
        public void onError(@NotNull McuMgrException e) {
            fail(e);
        }
    };

    /**
     * State: RESET.
     * Observer for the transport disconnection.
     */
    private final McuMgrTransport.ConnectionObserver mResetObserver = new McuMgrTransport.ConnectionObserver() {
        @Override
        public void onConnected() {
            // Do nothing
        }

        @Override
        public void onDisconnected() {
            mDefaultManager.getTransporter().removeObserver(mResetObserver);

            LOG.info("Device disconnected");
            Runnable reconnect = () -> mDefaultManager.getTransporter().connect(mReconnectCallback);
            // Calculate the delay needed before verification.
            // It may have taken 20 sec before the phone realized that it's
            // disconnected. No need to wait more, perhaps?
            long now = SystemClock.elapsedRealtime();
            long timeSinceReset = now - mResetResponseTime;
            long remainingTime = mEstimatedSwapTime - timeSinceReset;

            if (remainingTime > 0) {
                LOG.trace("Waiting for estimated swap time {}ms", mEstimatedSwapTime);
                new Handler(Looper.getMainLooper()).postDelayed(reconnect, remainingTime);
            } else {
                reconnect.run();
            }
        }
    };

    /**
     * State: RESET.
     * Callback for reconnecting to the device.
     */
    private final McuMgrTransport.ConnectionCallback mReconnectCallback = new McuMgrTransport.ConnectionCallback() {

        @Override
        public void onConnected() {
            LOG.info("Reconnect successful");
            continueUpgrade();
        }

        @Override
        public void onDeferred() {
            LOG.info("Reconnect deferred");
            continueUpgrade();
        }

        @Override
        public void onError(@NotNull Throwable t) {
            LOG.error("Reconnect failed");
            fail(new McuMgrException(t));
        }

        public void continueUpgrade() {
            switch (mState) {
                case NONE:
                    // Upload cancelled in state validate.
                    cancelled(State.VALIDATE);
                    break;
                case VALIDATE:
                    // If the reset occurred in the validate state, we must re-validate as
                    // multiple resets may be required.
                    validate();
                    break;
                case RESET:
                    switch (mMode) {
                        case TEST_AND_CONFIRM:
                            // The device reconnected after testing.
                            verify();
                            break;
                        case TEST_ONLY:
                        case CONFIRM_ONLY:
                            // The device has been tested or confirmed.
                            success();
                            break;
                    }
            }
        }
    };

    /**
     * State: RESET.
     * Callback for the reset command.
     */
    private final McuMgrCallback<McuMgrResponse> mResetCallback = new McuMgrCallback<McuMgrResponse>() {
        @Override
        public void onResponse(@NotNull McuMgrResponse response) {
            // Check for an error return code
            if (!response.isSuccess()) {
                fail(new McuMgrErrorException(response.getReturnCode()));
                return;
            }
            mResetResponseTime = SystemClock.elapsedRealtime();
            LOG.trace("Reset request success. Waiting for disconnect...");
        }

        @Override
        public void onError(@NotNull McuMgrException e) {
            fail(e);
        }
    };

    /**
     * State: CONFIRM.
     * Callback for the confirm command.
     */
    private final McuMgrCallback<McuMgrImageStateResponse> mConfirmCallback = new McuMgrCallback<McuMgrImageStateResponse>() {
        private final static int MAX_ATTEMPTS = 2;
        private int mAttempts = 0;

        @Override
        public void onResponse(@NotNull McuMgrImageStateResponse response) {
            // Reset retry counter
            mAttempts = 0;

            LOG.trace("Confirm response: {}", response.toString());
            // Check for an error return code
            if (!response.isSuccess()) {
                fail(new McuMgrErrorException(response.getReturnCode()));
                return;
            }
            if (response.images.length == 0) {
                fail(new McuMgrException("Confirm response does not contain enough info"));
                return;
            }
            // Handle the response based on mode.
            switch (mMode) {
                case CONFIRM_ONLY:
                    // Check that an image exists in slot 1
                    if (response.images.length != 2) {
                        fail(new McuMgrException("Confirm response does not contain enough info"));
                        return;
                    }
                    // Check that the upgrade image has been confirmed
                    if (!response.images[1].pending) {
                        fail(new McuMgrException("Image is not in a confirmed state."));
                        return;
                    }
                    // Reset the device, we don't want to do anything more.
                    reset();
                    break;
                case TEST_AND_CONFIRM:
                    // Check that the upgrade image has successfully booted
                    if (!Arrays.equals(mHash, response.images[0].hash)) {
                        fail(new McuMgrException("Device failed to boot into new image"));
                        return;
                    }
                    // Check that the upgrade image has been confirmed
                    if (!response.images[0].confirmed) {
                        fail(new McuMgrException("Image is not in a confirmed state."));
                        return;
                    }
                    // The device has been tested and confirmed.
                    success();
                    break;
            }
        }

        @Override
        public void onError(@NotNull McuMgrException e) {
            // The confirm request might have been sent after the device was rebooted
            // and the images were swapped. Swapping images, depending on the hardware,
            // make take a long time, during which the phone may throw 133 error as a
            // timeout. In such case we should try again.
            if (e instanceof McuMgrTimeoutException) {
                if (mAttempts++ < MAX_ATTEMPTS) {
                    // Try again
                    LOG.warn("Connection timeout. Retrying...");
                    verify();
                    return;
                }
            }
            fail(e);
        }
    };

    //******************************************************************
    // Firmware Upgrade State
    //******************************************************************

    public enum State {
        NONE, VALIDATE, UPLOAD, TEST, RESET, CONFIRM, SUCCESS;

        public boolean isInProgress() {
            return this == VALIDATE || this == UPLOAD || this == TEST ||
                    this == RESET || this == CONFIRM;
        }
    }

    /**
     * Get the current {@link State} of the firmware upgrade.
     *
     * @return The current state.
     */
    public State getState() {
        return mState;
    }

    /**
     * Called by {@link FirmwareUpgradeManager#resume} to run the current state.
     */
    private synchronized void currentState() {
        if (mPaused) {
            return;
        }
        switch (mState) {
            case NONE:
                return;
            case VALIDATE:
                validate();
                break;
            case UPLOAD:
                mUploadController.resume();
                break;
            case TEST:
                test();
                break;
            case RESET:
                reset();
                break;
            case CONFIRM:
                confirm();
                break;
        }
    }

    //******************************************************************
    // Image Upload Callback
    //******************************************************************

    /**
     * Image upload callback. Forwards upload callbacks to the FirmwareUpgradeCallback.
     */

    private final UploadCallback mImageUploadCallback = new UploadCallback() {

        @Override
        public void onUploadProgressChanged(int current, int total, long timestamp) {
            mInternalCallback.onUploadProgressChanged(current, total, timestamp);
        }

        @Override
        public void onUploadFailed(@NotNull McuMgrException error) {
            fail(error);
        }

        @Override
        public void onUploadCanceled() {
            cancelled(State.UPLOAD);
        }

        @Override
        public void onUploadCompleted() {
            // When upload is complete, send test on confirm commands, depending on the mode.
            switch (mMode) {
                case TEST_ONLY:
                case TEST_AND_CONFIRM:
                    test();
                    break;
                case CONFIRM_ONLY:
                    confirm();
                    break;
            }
        }
    };

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
