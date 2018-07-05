/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.dfu;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Arrays;
import java.util.concurrent.Executor;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.image.McuMgrImage;
import io.runtime.mcumgr.managers.DefaultManager;
import io.runtime.mcumgr.managers.ImageManager;
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.response.img.McuMgrImageStateResponse;

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
    private final static String TAG = "FirmwareUpgradeManager";

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
    private ImageManager mImageManager;

    /**
     * Performs the reset command.
     */
    private DefaultManager mDefaultManager;

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
     * Construct a firmware upgrade manager. If using this constructor, the callback must be set
     * using {@link #setFirmwareUpgradeCallback(FirmwareUpgradeCallback)} before calling
     * {@link FirmwareUpgradeManager#start}.
     *
     * @param transport the transporter to use.
     */
    public FirmwareUpgradeManager(@NonNull McuMgrTransport transport) {
        mState = State.NONE;
        mImageManager = new ImageManager(transport);
        mDefaultManager = new DefaultManager(transport);
    }

    /**
     * Construct a firmware upgrade manager.
     *
     * @param transport the transporter to use.
     * @param callback  the callback.
     */
    public FirmwareUpgradeManager(@NonNull McuMgrTransport transport,
                                  @NonNull FirmwareUpgradeCallback callback) {
        this(transport);
        mCallback = callback;
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
    public void setFirmwareUpgradeCallback(@NonNull FirmwareUpgradeCallback callback) {
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
    public void setMode(@NonNull Mode mode) {
        if (mState != State.NONE) {
            Log.i(TAG, "Firmware upgrade is already in progress");
            return;
        }
        mMode = mode;
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
     * Start the upgrade.
     * <p>
     * The specified image file will be sent to the target using the
     * given transport, then verified using test command. If test successful, the reset
     * command will be sent. The device should boot with the new firmware.
     * The manager will try to connect to the SMP server on the new firmware and confirm
     * the upload.
     */
    public synchronized void start(@NonNull byte[] imageData) throws McuMgrException {
        if (mState != State.NONE) {
            Log.i(TAG, "Firmware upgrade is already in progress");
            return;
        }
        // Set image and validate
        mImageData = imageData;
        mHash = McuMgrImage.getHash(imageData);

        // Begin the upload
        mInternalCallback.onStart(this);
        validate();
    }

    //******************************************************************
    // Upload Controller
    //******************************************************************

    @Override
    public synchronized void cancel() {
        if (mState.isInProgress()) {
            cancelPrivate();
        }
    }

    @Override
    public synchronized void pause() {
        if (mState.isInProgress()) {
            Log.i(TAG, "Pausing upgrade.");
            mPaused = true;
            if (mState == State.UPLOAD) {
                mImageManager.pauseUpload();
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
    public boolean isPaused() {
        return mPaused;
    }

    @Override
    public boolean isInProgress() {
        return mState.isInProgress() && !isPaused();
    }

    //******************************************************************
    // Implementation
    //******************************************************************

    private synchronized void setState(State newState) {
        State prevState = mState;
        mState = newState;
        if (newState != prevState) {
            Log.v(TAG, "Moving from state " + prevState.name() + " to state " + newState.name());
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
            mImageManager.upload(mImageData, mImageUploadCallback);
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
        mInternalCallback.onSuccess();
    }

    private synchronized void fail(McuMgrException error) {
        cancelPrivate();
        mInternalCallback.onFail(mState, error);
    }

    private synchronized void cancelPrivate() {
        if (mState == State.UPLOAD) {
            mImageManager.cancelUpload();
        }
        mState = State.NONE;
        mPaused = false;
    }

    //******************************************************************
    // McuManagerCallbacks
    //******************************************************************

    /**
     * State: VALIDATE.
     * Callback for the list command.
     */
    private McuMgrCallback<McuMgrImageStateResponse> mImageValidateCallback =
            new McuMgrCallback<McuMgrImageStateResponse>() {
                @Override
                public void onResponse(@NonNull final McuMgrImageStateResponse response) {
                    Log.v(TAG, "Validation response: " + response.toString());

                    // Check for an error return code
                    if (!response.isSuccess()) {
                        fail(new McuMgrErrorException(response.getRc()));
                        return;
                    }

                    McuMgrImageStateResponse.ImageSlot[] images = response.images;

                    // Check if the new firmware is different than the active one.
                    if (images.length > 0 && Arrays.equals(mHash, images[0].hash)) {
                        // The new firmware is already active. No need to do anything.
                        success();
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
                public void onError(@NonNull McuMgrException e) {
                    fail(e);
                }
            };

    /**
     * State: TEST.
     * Callback for the test command.
     */
    private McuMgrCallback<McuMgrImageStateResponse> mTestCallback = new McuMgrCallback<McuMgrImageStateResponse>() {
        @Override
        public void onResponse(@NonNull McuMgrImageStateResponse response) {
            Log.v(TAG, "Test response: " + response.toString());
            // Check for an error return code
            if (!response.isSuccess()) {
                fail(new McuMgrErrorException(response.getRc()));
                return;
            }
            if (response.images.length != 2) {
                fail(new McuMgrException("Test response does not contain enough info"));
                return;
            }
            if (!response.images[1].pending) {
                Log.e(TAG, "Tested image is not in a pending state.");
                fail(new McuMgrException("Tested image is not in a pending state."));
                return;
            }
            // Test image success, begin device reset.
            reset();
        }

        @Override
        public void onError(@NonNull McuMgrException e) {
            fail(e);
        }
    };

    /**
     * State: RESET.
     * Observer for the transport disconnection.
     */
    private McuMgrTransport.ConnectionObserver mResetObserver
            = new McuMgrTransport.ConnectionObserver() {
        @Override
        public void onConnected() {
            // ignore
        }

        @Override
        public void onDisconnected() {
            // Device has reset.
            mDefaultManager.getTransporter().removeObserver(this);
            Log.v(TAG, "Reset successful");
            switch (mMode) {
                case TEST_AND_CONFIRM:
                    // The device reconnected after testing.
                    confirm();
                    break;
                case TEST_ONLY:
                case CONFIRM_ONLY:
                    // The device has been tested or confirmed.
                    success();
                    break;
            }
        }
    };

    /**
     * State: RESET.
     * Callback for the reset command.
     */
    private McuMgrCallback<McuMgrResponse> mResetCallback = new McuMgrCallback<McuMgrResponse>() {
        @Override
        public void onResponse(@NonNull McuMgrResponse response) {
            // Reset command has been sent.
            Log.v(TAG, "Reset request sent. Waiting for reset");
            // Check for an error return code
            if (!response.isSuccess()) {
                fail(new McuMgrErrorException(response.getRc()));
            }
        }

        @Override
        public void onError(@NonNull McuMgrException e) {
            fail(e);
        }
    };

    /**
     * State: CONFIRM.
     * Callback for the confirm command.
     */
    private McuMgrCallback<McuMgrImageStateResponse> mConfirmCallback =
            new McuMgrCallback<McuMgrImageStateResponse>() {
                @Override
                public void onResponse(@NonNull McuMgrImageStateResponse response) {
                    Log.v(TAG, "Confirm response: " + response.toString());
                    // Check for an error return code
                    if (!response.isSuccess()) {
                        fail(new McuMgrErrorException(response.getRc()));
                        return;
                    }
                    if (response.images.length == 0) {
                        fail(new McuMgrException("Confirm response does not contain enough info"));
                        return;
                    }
                    if (!response.images[0].confirmed) {
                        Log.e(TAG, "Image is not in a confirmed state.");
                        fail(new McuMgrException("Image is not in a confirmed state."));
                        return;
                    }
                    // Confirm command has been sent.
                    switch (mMode) {
                        case CONFIRM_ONLY:
                            // Reset the device, we don't want to do anything more.
                            reset();
                            break;
                        case TEST_AND_CONFIRM:
                            // The device has been tested and confirmed.
                            success();
                            break;
                    }
                }

                @Override
                public void onError(@NonNull McuMgrException e) {
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
                   this == RESET    || this == CONFIRM;
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
                mImageManager.continueUpload();
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
    private ImageManager.ImageUploadCallback mImageUploadCallback =
            new ImageManager.ImageUploadCallback() {
        @Override
        public void onProgressChange(int bytesSent, int imageSize, long timestamp) {
            mInternalCallback.onUploadProgressChanged(bytesSent, imageSize, timestamp);
        }

        @Override
        public void onUploadFail(@NonNull McuMgrException error) {
            mInternalCallback.onFail(mState, error);
        }

        @Override
        public void onUploadCancel() {
            mInternalCallback.onCancel(mState);
        }

        @Override
        public void onUploadFinish() {
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
    // Main Thread Executor
    //******************************************************************

    /**
     * Internal callback to route callbacks to the UI thread if the flag has been set.
     */
    private FirmwareUpgradeCallback mInternalCallback = new FirmwareUpgradeCallback() {
        private MainThreadExecutor mMainThreadExecutor;

        private MainThreadExecutor getMainThreadExecutor() {
            if (mMainThreadExecutor == null) {
                mMainThreadExecutor = new MainThreadExecutor();
            }
            return mMainThreadExecutor;
        }

        @Override
        public void onStart(final FirmwareUpgradeController controller) {
            if (mUiThreadCallbacks) {
                getMainThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onStart(controller);
                    }
                });
            } else {
                mCallback.onStart(controller);
            }
        }

        @Override
        public void onStateChanged(final State prevState, final State newState) {
            if (mUiThreadCallbacks) {
                getMainThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onStateChanged(prevState, newState);
                    }
                });
            } else {
                mCallback.onStateChanged(prevState, newState);
            }
        }

        @Override
        public void onSuccess() {
            if (mUiThreadCallbacks) {
                getMainThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onSuccess();
                    }
                });
            } else {
                mCallback.onSuccess();
            }
        }

        @Override
        public void onFail(final State state, final McuMgrException error) {
            if (mUiThreadCallbacks) {
                getMainThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onFail(state, error);
                    }
                });
            } else {
                mCallback.onFail(state, error);
            }
        }

        @Override
        public void onCancel(final State state) {
            if (mUiThreadCallbacks) {
                getMainThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onCancel(state);
                    }
                });
            } else {
                mCallback.onCancel(state);
            }
        }

        @Override
        public void onUploadProgressChanged(final int bytesSent, final int imageSize, final long timestamp) {
            if (mUiThreadCallbacks) {
                getMainThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onUploadProgressChanged(bytesSent, imageSize, timestamp);
                    }
                });
            } else {
                mCallback.onUploadProgressChanged(bytesSent, imageSize, timestamp);
            }
        }
    };

    /**
     * Used to execute callbacks on the main UI thread.
     */
    private static class MainThreadExecutor implements Executor {
        private Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            mainThreadHandler.post(command);
        }
    }
}
