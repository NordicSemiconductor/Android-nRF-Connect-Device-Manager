/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.dfu;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import no.nordicsemi.android.mcumgr.dfu.mcuboot.FirmwareUpgradeManager;
import no.nordicsemi.android.mcumgr.exception.McuMgrException;

/**
 * Callbacks for firmware upgrades.
 */
public interface FirmwareUpgradeCallback<State> {

    /**
     * Called when the {@link FirmwareUpgradeManager} has started.
     * <p>
     * This callback is used to pass the upgrade controller which can pause/resume/cancel
     * an upgrade to a controller which may not have access to the original object.
     *
     * @param controller the upgrade controller.
     */
    void onUpgradeStarted(FirmwareUpgradeController controller);

    /**
     * Called when the firmware upgrade changes state.
     *
     * @param prevState previous state.
     * @param newState  new state.
     */
    void onStateChanged(State prevState, State newState);

    /**
     * Called when the firmware upgrade has succeeded.
     */
    void onUpgradeCompleted();

    /**
     * Called when the firmware upgrade has failed.
     *
     * @param state the state the upgrade failed in.
     * @param error the error.
     */
    void onUpgradeFailed(State state, McuMgrException error);

    /**
     * Called when the firmware upgrade has been canceled using the
     * {@link FirmwareUpgradeController#cancel()} method.
     * <p>
     * The upgrade may be cancelled only during uploading the image.
     *
     * @param state the state the upgrade was cancelled in.
     */
    void onUpgradeCanceled(State state);

    /**
     * Called when the upload state progress has changed.
     *
     * @param bytesSent the number of bytes sent so far.
     * @param imageSize the total number of bytes to send.
     * @param timestamp the time that the successful response packet for the progress was received.
     */
    void onUploadProgressChanged(int bytesSent, int imageSize, long timestamp);

    class Executor<State> implements FirmwareUpgradeCallback<State> {
        @NotNull
        private final MainThreadExecutor executor;

        @Nullable
        private FirmwareUpgradeCallback<State> callback;
        private boolean runOnIUThread = true;

        public Executor() {
            this.executor = new MainThreadExecutor();
        }

        public void setCallback(@Nullable FirmwareUpgradeCallback<State> callback) {
            this.callback = callback;
        }

        public void setRunOnIUThread(boolean runOnIUThread) {
            this.runOnIUThread = runOnIUThread;
        }

        @Override
        public void onUpgradeStarted(FirmwareUpgradeController controller) {
            final FirmwareUpgradeCallback<State> callback = this.callback;
            if (callback == null)
                return;
            if (runOnIUThread)
                executor.execute(() -> {
                    final FirmwareUpgradeCallback<State> cb = this.callback;
                    if (cb != null)
                        cb.onUpgradeStarted(controller);
                });
            else
                callback.onUpgradeStarted(controller);
        }

        @Override
        public void onStateChanged(State prevState, State newState) {
            final FirmwareUpgradeCallback<State> callback = this.callback;
            if (callback == null)
                return;
            if (runOnIUThread)
                executor.execute(() ->{
                    final FirmwareUpgradeCallback<State> cb = this.callback;
                    if (cb != null)
                        cb.onStateChanged(prevState, newState);
                });
            else
                callback.onStateChanged(prevState, newState);
        }

        @Override
        public void onUpgradeCompleted() {
            final FirmwareUpgradeCallback<State> callback = this.callback;
            if (callback == null)
                return;
            if (runOnIUThread)
                executor.execute(() -> {
                    final FirmwareUpgradeCallback<State> cb = this.callback;
                    if (cb != null)
                        cb.onUpgradeCompleted();
                });
            else
                callback.onUpgradeCompleted();
        }

        @Override
        public void onUpgradeFailed(State state, McuMgrException error) {
            final FirmwareUpgradeCallback<State> callback = this.callback;
            if (callback == null)
                return;
            if (runOnIUThread)
                executor.execute(() -> {
                    final FirmwareUpgradeCallback<State> cb = this.callback;
                    if (cb != null)
                        cb.onUpgradeFailed(state, error);
                });
            else
                callback.onUpgradeFailed(state, error);
        }

        @Override
        public void onUpgradeCanceled(State state) {
            final FirmwareUpgradeCallback<State> callback = this.callback;
            if (callback == null)
                return;
            if (runOnIUThread)
                executor.execute(() -> {
                    final FirmwareUpgradeCallback<State> cb = this.callback;
                    if (cb != null)
                        cb.onUpgradeCanceled(state);
                });
            else
                callback.onUpgradeCanceled(state);
        }

        @Override
        public void onUploadProgressChanged(int bytesSent, int imageSize, long timestamp) {
            final FirmwareUpgradeCallback<State> callback = this.callback;
            if (callback == null)
                return;
            if (runOnIUThread)
                executor.execute(() -> {
                    final FirmwareUpgradeCallback<State> cb = this.callback;
                    if (cb != null)
                        cb.onUploadProgressChanged(bytesSent, imageSize, timestamp);
                });
            else
                callback.onUploadProgressChanged(bytesSent, imageSize, timestamp);
        }
    }
}
