/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr;

import org.jetbrains.annotations.NotNull;

import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.McuMgrResponse;

/**
 * An McuMgrTransport is tasked with sending requests to, and receiving responses from a device.
 * Transport implementations should conform to one of the {@link McuMgrScheme}s. Furthermore, the
 * McuManager does not maintain the state of the transporter, and so the transport must set-up and
 * tear-down connections on its own accord.
 */
public interface McuMgrTransport {

    interface ConnectionObserver {
        /**
         * A method called when the connection to the device has been established.
         */
        void onConnected();

        /**
         * A method called when the connection to the device has been lost.
         */
        void onDisconnected();
    }

    /**
     * Gets the scheme for this transport (see {@link McuMgrScheme}).
     *
     * @return The transport's scheme.
     */
    @NotNull
    McuMgrScheme getScheme();

    /**
     * Send a synchronous Mcu Manager request. This method implementation should block until a
     * response has been received or a error has occurred.
     *
     * @param payload      the request packet data to send to the device.
     * @param responseType the response type.
     * @param <T>          the response type.
     * @return The response.
     * @throws McuMgrException thrown on error. Set the cause of the error if caused by a different
     *                         type of exception.
     */
    @NotNull
    <T extends McuMgrResponse> T send(@NotNull byte[] payload, @NotNull Class<T> responseType)
            throws McuMgrException;

    /**
     * Send an asynchronous Mcu Manager request. This method should not be blocked. When the
     * response has been received or an error occurs, the appropriate method of the callback should
     * be called.
     *
     * @param payload      the request packet data to send to the device.
     * @param responseType the response type.
     * @param callback     the callback to call on response or error.
     * @param <T>          the response type.
     */
    <T extends McuMgrResponse> void send(@NotNull byte[] payload, @NotNull Class<T> responseType,
                                         @NotNull McuMgrCallback<T> callback);

    /**
     * Releases the transport connection. When the connection is already closed this method does
     * nothing.
     */
    void release();

    /**
     * Adds the connection observer. An observer will be notified whenever the connected device
     * gets connected or disconnected.
     *
     * @param observer the observer.
     */
    void addObserver(@NotNull ConnectionObserver observer);

    /**
     * Removes previously registered observer.
     *
     * @param observer the observer.
     */
    void removeObserver(@NotNull ConnectionObserver observer);
}
