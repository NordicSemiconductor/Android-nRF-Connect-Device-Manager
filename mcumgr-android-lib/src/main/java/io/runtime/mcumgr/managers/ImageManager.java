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
import java.util.Date;
import java.util.HashMap;

import io.runtime.mcumgr.McuManager;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;
import io.runtime.mcumgr.exception.InsufficientMtuException;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.image.McuMgrImage;
import io.runtime.mcumgr.response.img.McuMgrImageStateResponse;
import io.runtime.mcumgr.response.img.McuMgrImageUploadResponse;
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.util.CBOR;

/**
 * Image command-group manager. This manager can read the image state of a device, test or
 * confirm images, and perform image uploads to the spare image slot. Once initialized, an
 * ImageManager can perform multiple image uploads, <b>but can only handle performing one at a
 * time.</b>
 * <p>
 * It is important to note that image upload is only one step in a firmware upgrade. To perform
 * a full firmware upgrade use {@link FirmwareUpgradeManager}.
 * @see FirmwareUpgradeManager
 */
public class ImageManager extends McuManager {

    private final static String TAG = "ImageManager";

    private final static int IMG_HASH_LEN = 32;

    // Image manager command IDs
    public final static int ID_STATE = 0;
    public final static int ID_UPLOAD = 1;
    public final static int ID_FILE = 2;
    public final static int ID_CORELIST = 3;
    public final static int ID_CORELOAD = 4;
    public final static int ID_ERASE = 5;

    /**
     * Construct an image manager.
     *
     * @param transport the transport to use to send commands.
     */
    public ImageManager(McuMgrTransport transport) {
        super(GROUP_IMAGE, transport);
    }

    /**
     * List the images on a device (asynchronous).
     * <p>
     * The response payload can be mapped to a {@link McuMgrImageStateResponse}.
     *
     * @param callback the asynchronous callback
     */
    public void list(McuMgrCallback<McuMgrImageStateResponse> callback) {
        send(OP_READ, ID_STATE, null, McuMgrImageStateResponse.class, callback);
    }

    /**
     * List the images on a device (synchronous).
     * <p>
     * The response payload can be mapped to a {@link McuMgrImageStateResponse}.
     *
     * @return the response
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrImageStateResponse list() throws McuMgrException {
        return send(OP_READ, ID_STATE, null, McuMgrImageStateResponse.class);
    }

    /**
     * Test an image on the device (asynchronous).
     * <p>
     * Testing an image will verify the image and put it in a pending state. That is, when the
     * device resets, the pending image will be booted into.
     *
     * @param hash     the hash of the image to test
     * @param callback the asynchronous callback
     */
    public void test(byte[] hash, McuMgrCallback<McuMgrImageStateResponse> callback) {
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
     * @param hash the hash of the image to test
     * @return the response
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrImageStateResponse test(byte[] hash) throws McuMgrException {
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
     * @param hash     the hash of the image to confirm
     * @param callback the asynchronous callback
     */
    public void confirm(byte[] hash, McuMgrCallback<McuMgrImageStateResponse> callback) {
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
     * @param hash the hash of the image to confirm
     * @return the response
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrImageStateResponse confirm(byte[] hash) throws McuMgrException {
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
     * @param data     the image data to upload to slot 1
     * @param callback the image upload callback
     */
    public synchronized void upload(@NonNull byte[] data, @NonNull ImageUploadCallback callback) {
        if (mUploadState == STATE_NONE) {
            mUploadState = STATE_UPLOADING;
        } else {
            Log.d(TAG, "An image upload is already in progress");
            return;
        }

        mUploadCallback = callback;
        mImageUploadData = data;

        sendUploadData(0);
    }

    /**
     * Erase the image in slot 1 (asynchronous).
     *
     * @param callback the asynchronous callback
     */
    public void erase(McuMgrCallback<McuMgrResponse> callback) {
        send(OP_WRITE, ID_STATE, null, McuMgrResponse.class, callback);
    }

    /**
     * Erase the image in slot 1 (synchronous).
     *
     * @return the response
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrResponse erase() throws McuMgrException {
        return send(OP_WRITE, ID_STATE, null, McuMgrResponse.class);
    }

    /**
     * Core list (asynchronous).
     *
     * @param callback the asynchronous callback
     */
    /* TODO : create the correct response class. I don't know what this does */
    public void coreList(McuMgrCallback<McuMgrResponse> callback) {
        send(OP_READ, ID_CORELIST, null, McuMgrResponse.class, callback);
    }

    /**
     * Core list (synchronous).
     *
     * @return the response
     * @throws McuMgrException Transport error. See cause.
     */
    /* TODO : create the correct response class. I don't know what this does */
    public McuMgrResponse coreList() throws McuMgrException {
        return send(OP_READ, ID_CORELIST, null, McuMgrResponse.class);
    }

    /**
     * Core load (asynchronous).
     *
     * @param offset   offset
     * @param callback the asynchronous callback
     */
    /* TODO : create the correct response class. I don't know what this does */
    public void coreLoad(int offset, McuMgrCallback<McuMgrResponse> callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("off", offset);
        send(OP_READ, ID_CORELOAD, payloadMap, McuMgrResponse.class, callback);
    }

    /**
     * Core load (synchronous).
     *
     * @param offset offset
     * @return the response
     * @throws McuMgrException Transport error. See cause.
     */
    /* TODO : create the correct response class. I don't know what this does */
    public McuMgrResponse coreLoad(int offset) throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("off", offset);
        return send(OP_READ, ID_CORELOAD, payloadMap, McuMgrResponse.class);
    }

    /**
     * Core erase (asynchronous).
     *
     * @param callback the asynchronous callback
     */
    /* TODO : create the correct response class. I don't know what this does */
    public void coreErase(McuMgrCallback<McuMgrResponse> callback) {
        send(OP_WRITE, ID_CORELOAD, null, McuMgrResponse.class, callback);
    }

    /**
     * Core erase (synchronous).
     *
     * @return the response
     * @throws McuMgrException Transport error. See cause.
     */
    /* TODO : create the correct response class. I don't know what this does */
    public McuMgrResponse coreErase() throws McuMgrException {
        return send(OP_WRITE, ID_CORELOAD, null, McuMgrResponse.class);
    }

    //******************************************************************
    // Image Upload
    //******************************************************************

    // Image Upload Constants
    private final static int DEFAULT_MTU = 512;

    // Upload states
    public final static int STATE_NONE = 0;
    public final static int STATE_UPLOADING = 1;
    public final static int STATE_PAUSED = 2;

    // Upload variables
    private int mUploadState = STATE_NONE;
    private int mUploadOffset = 0;
    private byte[] mImageUploadData;
    private ImageUploadCallback mUploadCallback;
    private int mMtu = DEFAULT_MTU;

    /**
     * Sets the upload MTU.
     *
     * @param mtu the MTU to use for image upload
     */
    public synchronized boolean setUploadMtu(int mtu) {
        Log.v(TAG, "Setting image upload MTU");
        if (mUploadState == STATE_UPLOADING) {
            Log.e(TAG, "Upload must not be in progress!");
            return false;
        } else if (mtu < 23) {
            Log.e(TAG, "MTU is too small!");
            return false;
        } else if (mtu > 1024) {
            Log.e(TAG, "MTU is too large!");
            return false;
        } else {
            mMtu = mtu;
            return true;
        }
    }

    /**
     * Get the current upload state. ({@link ImageManager#STATE_NONE},
     * {@link ImageManager#STATE_UPLOADING}, {@link ImageManager#STATE_PAUSED}).
     * @return the current upload state.
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
        } else {
            resetUpload();
        }
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
            sendUploadData(mUploadOffset);
        } else {
            Log.d(TAG, "Upload is not paused.");
        }
    }


    private synchronized void failUpload(McuMgrException error) {
        if (mUploadCallback != null) {
            mUploadCallback.onUploadFail(error);
        }
        cancelUpload();
    }

    private synchronized void restartUpload() {
        if (mImageUploadData == null || mUploadCallback == null) {
            Log.e(TAG, "Could not restart upload: image data or callback is null!");
            return;
        }
        byte[] tempData = mImageUploadData;
        ImageUploadCallback tempCallback = mUploadCallback;
        resetUpload();
        upload(tempData, tempCallback);
        return;
    }

    private synchronized void resetUpload() {
        mUploadState = STATE_NONE;
        mUploadOffset = 0;
        mImageUploadData = null;
        mUploadCallback = null;
    }

    /**
     * Send a packet of upload data from the specified offset
     * @param offset the image data offset to send data from
     */
    private synchronized void sendUploadData(int offset) {
        // Check that the state is STATE_UPLOADING
        if (mUploadState != STATE_UPLOADING) {
            Log.d(TAG, "Image upload is not in the UPLOADING state.");
            return;
        }

        // Get the length of data (in bytes) to put into the upload packet. This calculated as:
        // min(MTU - packetOverhead, imageLength - uploadOffset)
        Log.v(TAG, "Send upload data at offset: " + offset);
        int dataLength = Math.min(mMtu - calculatePacketOverhead(mImageUploadData, offset),
                mImageUploadData.length - offset);
        Log.v(TAG, "Image data length: " + dataLength);

        // Copy the data from the image into a buffer.
        byte[] sendBuffer = new byte[dataLength];
        for (int i = 0; i < dataLength; i++) {
            sendBuffer[i] = mImageUploadData[offset + i];
        }

        // Create the map of key-values for the McuManager payload
        HashMap<String, Object> payloadMap = new HashMap<>();
        // Put the data and offset
        payloadMap.put("data", sendBuffer);
        payloadMap.put("off", offset);
        if (offset == 0) {
            // Only send the length of the image in the first packet of the upload
            payloadMap.put("len", mImageUploadData.length);
        }

        // Send the request
        send(OP_WRITE, ID_UPLOAD, payloadMap, McuMgrImageUploadResponse.class, mCallback);
    }

    /**
     * The upload which is called after a successful call to
     * {@link ImageManager#sendUploadData(int)}'s response has been received or an error has
     * occurred. On success, this callback parses the response, calls the upload progress callback
     * and sends the next packet of image data from the offset specified in the response. On error,
     * the upload is failed unless the error specifies that the packet sent to the transporter was
     * too large to send ({@link InsufficientMtuException}). In this case, the MTU is set to the
     * MTU in the exception and the upload is restarted.
     */
    private McuMgrCallback<McuMgrImageUploadResponse> mCallback =
            new McuMgrCallback<McuMgrImageUploadResponse>() {
        @Override
        public void onResponse(McuMgrImageUploadResponse response) {
            // Check for a McuManager error
            if (response.rc != 0) {
                // TODO when the image in slot 1 is confirmed, this will return ENOMEM (2).
                Log.e(TAG, "Upload failed due to McuManager error: " + response.rc);
                failUpload(new McuMgrErrorException(McuMgrErrorCode.valueOf(response.rc)));
                return;
            }

            // Get the next offset to send image data from
            mUploadOffset = response.off;

            // Call the progress callback
            mUploadCallback.onProgressChange(mUploadOffset, mImageUploadData.length, new Date());

            // Check if the upload has finished.
            if (mUploadOffset == mImageUploadData.length) {
                Log.d(TAG, "Upload finished!");
                mUploadCallback.onUploadFinish();
                return;
            }

            // Send the next packet of upload data from the offset provided in the response.
            sendUploadData(mUploadOffset);
        }

        @Override
        public void onError(McuMgrException error) {
            // TODO if the Mtu is set successfully but the MTU is still insufficient, this will loop forever
            // TODO add a maximum number of restarts due to MTU until failing
            // Check if the exception is due to an insufficient MTU.
            if (error instanceof InsufficientMtuException) {
                InsufficientMtuException mtuErr = (InsufficientMtuException) error;

                // Pause the upload
                pauseUpload();

                // Set the MTU to the value specified in the error response.
                boolean isMtuSet = setUploadMtu(mtuErr.getMtu());

                if (isMtuSet) {
                    // If the MTU has been set successfully, restart the upload.
                    restartUpload();
                    return;
                }
            }
            // If the exception is not due to insufficient MTU fail the upload
            failUpload(error);
        }
    };

    // TODO more precise overhead calculations
    private int calculatePacketOverhead(byte[] data, int offset) {
        HashMap<String, Object> overheadTestMap = new HashMap<>();
        byte[] nmgrHeader = {0, 0, 0, 0, 0, 0, 0, 0};
        overheadTestMap.put("data", new byte[0]);
        overheadTestMap.put("off", offset);
        if (offset == 0) {
            overheadTestMap.put("len", data.length);
        }
        try {
            if (getScheme().isCoap()) {
                overheadTestMap.put("_h", nmgrHeader);
                byte[] cborData = CBOR.toBytes(overheadTestMap);
                // 20 byte estimate of CoAP Header; 5 bytes for good measure
                return cborData.length + 20 + 5;
            } else {
                byte[] cborData = CBOR.toBytes(overheadTestMap);
                // 8 bytes for McuMgr header; 5 bytes for good measure
                return cborData.length + 8 + 5;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static byte[] getHashFromImage(byte[] data) throws McuMgrException {
        return McuMgrImage.getHash(data);
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
         * @param bytesSent the number of bytes sent so far
         * @param imageSize the size of the image in bytes
         * @param ts        the timestamp of when the response was received
         */
        void onProgressChange(int bytesSent, int imageSize, Date ts);

        /**
         * Called when the upload has failed
         *
         * @param error the error. See the cause for more info.
         */
        void onUploadFail(McuMgrException error);

        /**
         * Called when the upload has finished successfully.
         */
        void onUploadFinish();
    }
}
