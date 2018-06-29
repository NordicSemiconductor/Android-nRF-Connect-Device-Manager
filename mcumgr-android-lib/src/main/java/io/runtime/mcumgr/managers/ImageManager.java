/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.managers;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;

import io.runtime.mcumgr.McuManager;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;
import io.runtime.mcumgr.exception.InsufficientMtuException;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.response.img.McuMgrCoreLoadResponse;
import io.runtime.mcumgr.response.img.McuMgrImageStateResponse;
import io.runtime.mcumgr.response.img.McuMgrImageUploadResponse;
import io.runtime.mcumgr.util.CBOR;

/**
 * Image command-group manager. This manager can read the image state of a device, test or
 * confirm images, and perform image uploads to the spare image slot. Once initialized, an
 * ImageManager can perform multiple image uploads, <b>but can only handle performing one at a
 * time.</b>
 * <p>
 * It is important to note that image upload is only one step in a firmware upgrade. To perform
 * a full firmware upgrade use {@link FirmwareUpgradeManager}.
 *
 * @see FirmwareUpgradeManager
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ImageManager extends McuManager {
    private final static String TAG = "ImageManager";

    private final static int IMG_HASH_LEN = 32;

    // Image manager command IDs
    private final static int ID_STATE = 0;
    private final static int ID_UPLOAD = 1;
    private final static int ID_FILE = 2;
    private final static int ID_CORELIST = 3;
    private final static int ID_CORELOAD = 4;
    private final static int ID_ERASE = 5;
    private final static int ID_ERASE_STATE = 6;

    /**
     * Construct an image manager.
     *
     * @param transport the transport to use to send commands.
     */
    public ImageManager(@NonNull McuMgrTransport transport) {
        super(GROUP_IMAGE, transport);
    }

    /**
     * List the images on a device (asynchronous).
     * <p>
     * The response payload can be mapped to a {@link McuMgrImageStateResponse}.
     *
     * @param callback the asynchronous callback.
     */
    public void list(@NonNull McuMgrCallback<McuMgrImageStateResponse> callback) {
        send(OP_READ, ID_STATE, null, McuMgrImageStateResponse.class, callback);
    }

    /**
     * List the images on a device (synchronous).
     * <p>
     * The response payload can be mapped to a {@link McuMgrImageStateResponse}.
     *
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NonNull
    public McuMgrImageStateResponse list() throws McuMgrException {
        return send(OP_READ, ID_STATE, null, McuMgrImageStateResponse.class);
    }

    /**
     * Send a packet of given data from the specified offset to the device (asynchronous).
     * <p>
     * The chunk size is limited by the current MTU. If the current MTU set by
     * {@link #setUploadMtu(int)} is too large, the {@link McuMgrCallback#onError(McuMgrException)}
     * with {@link InsufficientMtuException} error will be returned.
     * Use {@link InsufficientMtuException#getMtu()} to get the current MTU and
     * pass it to {@link #setUploadMtu(int)} and try again.
     * <p>
     * Use {@link #upload(byte[], ImageUploadCallback)} to send the whole file asynchronously
     * using one command.
     *
     * @param data     image data.
     * @param offset   the offset, from which the chunk will be sent.
     * @param callback the asynchronous callback.
     * @see #upload(byte[], ImageUploadCallback)
     */
    public void upload(byte[] data, int offset, @NonNull McuMgrCallback<McuMgrImageUploadResponse> callback) {
        // Get the length of data (in bytes) to put into the upload packet. This calculated as:
        // min(MTU - packetOverhead, imageLength - uploadOffset)
        int dataLength = Math.min(mMtu - calculatePacketOverhead(data, offset),
                data.length - offset);

        // Copy the data from the image into a buffer.
        byte[] sendBuffer = new byte[dataLength];
        System.arraycopy(data, offset, sendBuffer, 0, dataLength);

        // Create the map of key-values for the McuManager payload
        HashMap<String, Object> payloadMap = new HashMap<>();
        // Put the data and offset
        payloadMap.put("data", sendBuffer);
        payloadMap.put("off", offset);
        if (offset == 0) {
            // Only send the length of the image in the first packet of the upload
            payloadMap.put("len", data.length);
        }

        // Send the request
        send(OP_WRITE, ID_UPLOAD, payloadMap, McuMgrImageUploadResponse.class, callback);
    }

    /**
     * Send a packet of given data from the specified offset to the device (synchronous).
     * <p>
     * The chunk size is limited by the current MTU. If the current MTU set by
     * {@link #setUploadMtu(int)} is too large, the {@link InsufficientMtuException} error will be
     * thrown. Use {@link InsufficientMtuException#getMtu()} to get the current MTU and
     * pass it to {@link #setUploadMtu(int)} and try again.
     * <p>
     * Use {@link #upload(byte[], ImageUploadCallback)} to send the whole file asynchronously
     * using one command.
     *
     * @param data   image data.
     * @param offset the offset, from which the chunk will be sent.
     * @return The upload response.
     * @see #upload(byte[], ImageUploadCallback)
     */
    public McuMgrImageUploadResponse upload(byte[] data, int offset) throws McuMgrException {
        // Get the length of data (in bytes) to put into the upload packet. This calculated as:
        // min(MTU - packetOverhead, imageLength - uploadOffset)
        int dataLength = Math.min(mMtu - calculatePacketOverhead(data, offset),
                data.length - offset);

        // Copy the data from the image into a buffer.
        byte[] sendBuffer = new byte[dataLength];
        System.arraycopy(data, offset, sendBuffer, 0, dataLength);

        // Create the map of key-values for the McuManager payload
        HashMap<String, Object> payloadMap = new HashMap<>();
        // Put the data and offset
        payloadMap.put("data", sendBuffer);
        payloadMap.put("off", offset);
        if (offset == 0) {
            // Only send the length of the image in the first packet of the upload
            payloadMap.put("len", data.length);
        }

        // Send the request
        return send(OP_WRITE, ID_UPLOAD, payloadMap, McuMgrImageUploadResponse.class);
    }

    /**
     * Test an image on the device (asynchronous).
     * <p>
     * Testing an image will verify the image and put it in a pending state. That is, when the
     * device resets, the pending image will be booted into.
     *
     * @param hash     the hash of the image to test.
     * @param callback the asynchronous callback.
     */
    public void test(@NonNull byte[] hash, @NonNull McuMgrCallback<McuMgrImageStateResponse> callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("hash", hash);
        payloadMap.put("confirm", false);
        send(OP_WRITE, ID_STATE, payloadMap, McuMgrImageStateResponse.class, callback);
    }

    /**
     * Test an image on the device (synchronous).
     * <p>
     * Testing an image will verify the image and put it in a pending state. That is, when the
     * device resets, the pending image will be booted into.
     *
     * @param hash the hash of the image to test.
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NonNull
    public McuMgrImageStateResponse test(@NonNull byte[] hash) throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("hash", hash);
        payloadMap.put("confirm", false);
        return send(OP_WRITE, ID_STATE, payloadMap, McuMgrImageStateResponse.class);
    }

    /**
     * Confirm an image on the device (asynchronous).
     * <p>
     * Confirming an image will make it the default to boot into.
     *
     * @param hash     the hash of the image to confirm.
     * @param callback the asynchronous callback.
     */
    public void confirm(@NonNull byte[] hash, @NonNull McuMgrCallback<McuMgrImageStateResponse> callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("hash", hash);
        payloadMap.put("confirm", true);
        send(OP_WRITE, ID_STATE, payloadMap, McuMgrImageStateResponse.class, callback);
    }

    /**
     * Confirm an image on the device (synchronous).
     * <p>
     * Confirming an image will make it the default to boot into.
     *
     * @param hash the hash of the image to confirm.
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrImageStateResponse confirm(@NonNull byte[] hash) throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("hash", hash);
        payloadMap.put("confirm", true);
        return send(OP_WRITE, ID_STATE, payloadMap, McuMgrImageStateResponse.class);
    }

    /**
     * Begin an image upload.
     * <p>
     * Only one upload can occur per ImageManager.
     *
     * @param data     the image data to upload to slot 1.
     * @param callback the image upload callback.
     */
    public synchronized void upload(@NonNull byte[] data, @NonNull ImageUploadCallback callback) {
        if (mUploadState == STATE_NONE) {
            mUploadState = STATE_UPLOADING;
        } else {
            Log.d(TAG, "An image upload is already in progress");
            return;
        }

        mUploadCallback = callback;
        mImageData = data;

        sendNext(0);
    }

    /**
     * Erase the image in slot 1 (asynchronous).
     *
     * @param callback the asynchronous callback.
     */
    public void erase(McuMgrCallback<McuMgrResponse> callback) {
        send(OP_WRITE, ID_ERASE, null, McuMgrResponse.class, callback);
    }

    /**
     * Erase the image in slot 1 (synchronous).
     *
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrResponse erase() throws McuMgrException {
        return send(OP_WRITE, ID_ERASE, null, McuMgrResponse.class);
    }

    /**
     * Erase the state of image in slot 1 (asynchronous).
     *
     * @param callback the asynchronous callback.
     */
    public void eraseState(McuMgrCallback<McuMgrResponse> callback) {
        send(OP_WRITE, ID_ERASE_STATE, null, McuMgrResponse.class, callback);
    }

    /**
     * Erase the state of image in slot 1 (synchronous).
     *
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrResponse eraseState() throws McuMgrException {
        return send(OP_WRITE, ID_ERASE_STATE, null, McuMgrResponse.class);
    }

    /**
     * Core list (asynchronous).
     *
     * @param callback the asynchronous callback.
     */
    public void coreList(McuMgrCallback<McuMgrResponse> callback) {
        send(OP_READ, ID_CORELIST, null, McuMgrResponse.class, callback);
    }

    /**
     * Core list (synchronous).
     *
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrResponse coreList() throws McuMgrException {
        return send(OP_READ, ID_CORELIST, null, McuMgrResponse.class);
    }

    /**
     * Core load (asynchronous).
     *
     * @param offset   offset.
     * @param callback the asynchronous callback.
     */
    public void coreLoad(int offset, McuMgrCallback<McuMgrCoreLoadResponse> callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("off", offset);
        send(OP_READ, ID_CORELOAD, payloadMap, McuMgrCoreLoadResponse.class, callback);
    }

    /**
     * Core load (synchronous).
     *
     * @param offset offset.
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrCoreLoadResponse coreLoad(int offset) throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("off", offset);
        return send(OP_READ, ID_CORELOAD, payloadMap, McuMgrCoreLoadResponse.class);
    }

    /**
     * Core erase (asynchronous).
     *
     * @param callback the asynchronous callback.
     */
    public void coreErase(McuMgrCallback<McuMgrResponse> callback) {
        send(OP_WRITE, ID_CORELOAD, null, McuMgrResponse.class, callback);
    }

    /**
     * Core erase (synchronous).
     *
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrResponse coreErase() throws McuMgrException {
        return send(OP_WRITE, ID_CORELOAD, null, McuMgrResponse.class);
    }

    //******************************************************************
    // Image Upload
    //******************************************************************

    // Upload states
    public final static int STATE_NONE = 0;
    public final static int STATE_UPLOADING = 1;
    public final static int STATE_PAUSED = 2;

    // Upload variables
    private int mUploadState = STATE_NONE;
    private int mUploadOffset = 0;
    private byte[] mImageData;
    private ImageUploadCallback mUploadCallback;

    /**
     * Get the current upload state ({@link ImageManager#STATE_NONE},
     * {@link ImageManager#STATE_UPLOADING}, {@link ImageManager#STATE_PAUSED}).
     *
     * @return The current upload state.
     */
    public synchronized int getUploadState() {
        return mUploadState;
    }

    /**
     * Cancel an image upload. Does nothing if an image upload is not in progress.
     */
    public synchronized void cancelUpload() {
        if (mUploadState == STATE_NONE) {
            Log.d(TAG, "Image upload is not in progress");
        } else if (mUploadState == STATE_PAUSED) {
            Log.d(TAG, "Upload canceled!");
            resetUpload();
            mUploadCallback.onUploadCancel();
            mUploadCallback = null;
        }
        mUploadState = STATE_NONE;
    }

    /**
     * Pause an in progress upload.
     */
    public synchronized void pauseUpload() {
        if (mUploadState == STATE_NONE) {
            Log.d(TAG, "Upload is not in progress.");
        } else {
            Log.d(TAG, "Upload paused.");
            mUploadState = STATE_PAUSED;
        }
    }

    /**
     * Continue a paused image upload.
     */
    public synchronized void continueUpload() {
        if (mUploadState == STATE_PAUSED) {
            Log.d(TAG, "Continuing upload.");
            mUploadState = STATE_UPLOADING;
            sendNext(mUploadOffset);
        } else {
            Log.d(TAG, "Upload is not paused.");
        }
    }

    //******************************************************************
    // Implementation
    //******************************************************************

    private synchronized void failUpload(McuMgrException error) {
        if (mUploadCallback != null) {
            mUploadCallback.onUploadFail(error);
        }
        cancelUpload();
    }

    private synchronized void restartUpload() {
        if (mImageData == null || mUploadCallback == null) {
            Log.e(TAG, "Could not restart upload: image data or callback is null!");
            return;
        }
        byte[] tempData = mImageData;
        ImageUploadCallback tempCallback = mUploadCallback;
        resetUpload();
        upload(tempData, tempCallback);
    }

    private synchronized void resetUpload() {
        mUploadState = STATE_NONE;
        mUploadOffset = 0;
        mImageData = null;
    }

    /**
     * Send a packet of upload data from the specified offset.
     *
     * @param offset the image data offset to send data from.
     */
    private synchronized void sendNext(int offset) {
        // Check that the state is STATE_UPLOADING.
        if (mUploadState != STATE_UPLOADING) {
            Log.d(TAG, "Image Manager is not in the UPLOADING state.");
            return;
        }
        upload(mImageData, offset, mUploadCallbackImpl);
    }

    /**
     * The upload callback which is called after a {@link #sendNext(int)}'s response has been
     * received or an error has occurred. On success, this callback parses the response, calls the
     * upload progress callback and sends the next packet of image data from the offset specified
     * in the response. On error, the upload is failed unless the error specifies that the packet
     * sent to the transporter was too large to send ({@link InsufficientMtuException}).
     * In this case, the MTU is set to the MTU in the exception and the upload is restarted.
     */
    private final McuMgrCallback<McuMgrImageUploadResponse> mUploadCallbackImpl =
            new McuMgrCallback<McuMgrImageUploadResponse>() {
                @Override
                public void onResponse(@NonNull McuMgrImageUploadResponse response) {
                    // Check for a McuManager error.
                    if (response.rc != 0) {
                        // TODO when the image in slot 1 is confirmed, this will return ENOMEM (2).
                        Log.e(TAG, "Upload failed due to McuManager error: " + response.rc);
                        failUpload(new McuMgrErrorException(McuMgrErrorCode.valueOf(response.rc)));
                        return;
                    }

                    // Get the next offset to send image data from.
                    mUploadOffset = response.off;

                    // Call the progress callback.
                    mUploadCallback.onProgressChange(mUploadOffset, mImageData.length,
                            System.currentTimeMillis());

                    if (mUploadState == STATE_NONE) {
                        Log.d(TAG, "Upload canceled!");
                        resetUpload();
                        mUploadCallback.onUploadCancel();
                        mUploadCallback = null;
                        return;
                    }

                    // Check if the upload has finished.
                    if (mUploadOffset == mImageData.length) {
                        Log.d(TAG, "Upload finished!");
                        resetUpload();
                        mUploadCallback.onUploadFinish();
                        mUploadCallback = null;
                        return;
                    }

                    // Send the next packet of upload data from the offset provided in the response.
                    sendNext(mUploadOffset);
                }

                @Override
                public void onError(@NonNull McuMgrException error) {
                    // Check if the exception is due to an insufficient MTU.
                    if (error instanceof InsufficientMtuException) {
                        InsufficientMtuException mtuErr = (InsufficientMtuException) error;

                        // Set the MTU to the value specified in the error response.
                        int mtu = mtuErr.getMtu();
                        if (mMtu == mtu)
                            mtu -= 1;
                        boolean isMtuSet = setUploadMtu(mtu);

                        if (isMtuSet) {
                            // If the MTU has been set successfully, restart the upload.
                            restartUpload();
                            return;
                        }
                    }
                    // If the exception is not due to insufficient MTU fail the upload.
                    failUpload(error);
                }
            };

    // TODO more precise overhead calculations
    private int calculatePacketOverhead(@NonNull byte[] data, int offset) {
        HashMap<String, Object> overheadTestMap = new HashMap<>();
        overheadTestMap.put("data", new byte[0]);
        overheadTestMap.put("off", offset);
        if (offset == 0) {
            overheadTestMap.put("len", data.length);
        }
        try {
            if (getScheme().isCoap()) {
                byte[] header = {0, 0, 0, 0, 0, 0, 0, 0};
                overheadTestMap.put("_h", header);
                byte[] cborData = CBOR.toBytes(overheadTestMap);
                // 20 byte estimate of CoAP Header; 5 bytes for good measure
                return cborData.length + 20 + 5;
            } else {
                byte[] cborData = CBOR.toBytes(overheadTestMap);
                // 8 bytes for McuMgr header; 2 bytes for data length; 3 for command type and att ID
                return cborData.length + 8 + 2 + 3;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error while calculating packet overhead", e);
        }
        return -1;
    }

    //******************************************************************
    // Image Upload Callback
    //******************************************************************

    /**
     * Callback for upload command.
     */
    public interface ImageUploadCallback {

        /**
         * Called when a response has been received successfully.
         *
         * @param bytesSent the number of bytes sent so far.
         * @param imageSize the size of the image in bytes.
         * @param timestamp the timestamp of when the response was received.
         */
        void onProgressChange(int bytesSent, int imageSize, long timestamp);

        /**
         * Called when the upload has failed.
         *
         * @param error the error. See the cause for more info.
         */
        void onUploadFail(@NonNull McuMgrException error);

        /**
         * Called when the upload has been canceled.
         */
        void onUploadCancel();

        /**
         * Called when the upload has finished successfully.
         */
        void onUploadFinish();
    }
}
