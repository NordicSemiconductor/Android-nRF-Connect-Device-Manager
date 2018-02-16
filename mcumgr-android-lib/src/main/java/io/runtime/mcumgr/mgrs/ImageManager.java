/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.mgrs;

import android.util.Log;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

import io.runtime.mcumgr.McuManager;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.McuMgrScheme;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.resp.McuMgrImageStateResponse;
import io.runtime.mcumgr.resp.McuMgrImageUploadResponse;
import io.runtime.mcumgr.resp.McuMgrSimpleResponse;
import io.runtime.mcumgr.tlv.McuMgrImageTlvParser;
import io.runtime.mcumgr.util.CBOR;

import static io.runtime.mcumgr.McuMgrConstants.GROUP_IMAGE;
import static io.runtime.mcumgr.McuMgrConstants.OP_READ;
import static io.runtime.mcumgr.McuMgrConstants.OP_WRITE;

/**
 * Image command group manager.
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
     * Construct an image manager.
     *
     * @param transport the transport to use to send commands.
     * @param payload   The image to send (we need it right away, because set {@link #setUploadMtu(int)} needs it
     */
    public ImageManager(McuMgrTransport transport, byte[] payload) {
        super(GROUP_IMAGE, transport);
        mImageUploadData = payload;
    }

    /**
     * List the images on a device (asynchronous).
     * <p>
     * The response payload can be mapped to a {@link io.runtime.mcumgr.resp.McuMgrImageStateResponse}.
     *
     * @param callback the asynchronous callback
     */
    public void list(McuMgrCallback<McuMgrImageStateResponse> callback) {
        send(OP_READ, ID_STATE, null, McuMgrImageStateResponse.class, callback);
    }

    /**
     * List the images on a device (synchronous).
     * <p>
     * The response payload can be mapped to a {@link io.runtime.mcumgr.resp.McuMgrImageStateResponse}.
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
    public synchronized void upload(byte[] data, ImageUploadCallback callback) {
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
    public void erase(McuMgrCallback<McuMgrSimpleResponse> callback) {
        send(OP_WRITE, ID_STATE, null, McuMgrSimpleResponse.class,
                callback);
    }

    /**
     * Erase the image in slot 1 (synchronous).
     *
     * @return the response
     * @throws McuMgrException Transport error. See cause.
     */
    public McuMgrSimpleResponse erase() throws McuMgrException {
        return send(OP_WRITE, ID_STATE, null, McuMgrSimpleResponse.class);
    }

    /**
     * Core list (asynchronous).
     *
     * @param callback the asynchronous callback
     */
        /* TODO : create the correct response class */
    public void coreList(McuMgrCallback<McuMgrSimpleResponse> callback) {
        send(OP_READ, ID_CORELIST, null, McuMgrSimpleResponse.class, callback);
    }

    /**
     * Core list (synchronous).
     *
     * @return the response
     * @throws McuMgrException Transport error. See cause.
     */
        /* TODO : create the correct response class */
    public McuMgrSimpleResponse coreList() throws McuMgrException {
        return send(OP_READ, ID_CORELIST, null, McuMgrSimpleResponse.class);
    }

    /**
     * Core load (asynchronous).
     *
     * @param offset   offset
     * @param callback the asynchronous callback
     */
	/* TODO : create the correct response class */
    public void coreLoad(int offset, McuMgrCallback<McuMgrSimpleResponse> callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("off", offset);
        send(OP_READ, ID_CORELOAD, payloadMap, McuMgrSimpleResponse.class, callback);
    }

    /**
     * Core load (synchronous).
     *
     * @param offset offset
     * @return the response
     * @throws McuMgrException Transport error. See cause.
     */
	/* TODO : create the correct response class */
    public McuMgrSimpleResponse coreLoad(int offset) throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("off", offset);
        return send(OP_READ, ID_CORELOAD, payloadMap, McuMgrSimpleResponse.class);
    }

    /**
     * Core erase (asynchronous).
     *
     * @param callback the asynchronous callback
     */
		/* TODO : create the correct response class */
    public void coreErase(McuMgrCallback<McuMgrSimpleResponse> callback) {
        send(OP_WRITE, ID_CORELOAD, null, McuMgrSimpleResponse.class, callback);
    }

    /**
     * Core erase (synchronous).
     *
     * @return the response
     * @throws McuMgrException Transport error. See cause.
     */
		/* TODO : create the correct response class */
    public McuMgrSimpleResponse coreErase() throws McuMgrException {
        return send(OP_WRITE, ID_CORELOAD, null, McuMgrSimpleResponse.class);
    }


    //******************************************************************
    // Image Upload
    //******************************************************************

    // Image Upload Constants
    private final static int DEFAULT_MTU = 256;

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

    public synchronized void setUploadMtu(int mtu) {
        if (mUploadState == STATE_UPLOADING) {
            Log.e(TAG, "Upload must not be in progress!");
        } else if (mtu < calculatePacketOverhead(mImageUploadData, mUploadOffset) + 1) {
            Log.e(TAG, "Mtu is too small!");
        } else {
            mMtu = mtu;
        }
    }

    public synchronized int getUploadState() {
        return mUploadState;
    }

    public synchronized void cancelUpload() {
        if (mUploadState == STATE_NONE) {
            Log.d(TAG, "Image upload is not in progress");
        } else {
            resetUpload();
        }
    }

    private synchronized void cancelUpload(McuMgrException error) {
        if (mUploadCallback != null) {

            mUploadCallback.onUploadFail(error);
        }
        cancelUpload();
    }

    public synchronized void pauseUpload() {
        if (mUploadState == STATE_NONE) {
            Log.d(TAG, "Upload is not in progress.");
        } else {
            Log.d(TAG, "Upload paused.");
            mUploadState = STATE_PAUSED;
        }
    }

    public synchronized void continueUpload() {
        if (mUploadState == STATE_PAUSED) {
            Log.d(TAG, "Continuing upload.");
            mUploadState = STATE_UPLOADING;
            sendUploadData(mUploadOffset);
        } else {
            Log.d(TAG, "Upload is not paused.");
        }
    }

    private synchronized void resetUpload() {
        mUploadState = STATE_NONE;
        mUploadOffset = 0;
        mImageUploadData = null;
        mUploadCallback = null;
    }

    private synchronized void sendUploadData(int offset) {
        if (mUploadState != STATE_UPLOADING) {
            Log.d(TAG, "Image upload is not in the UPLOADING state.");
            return;
        }

        Log.v(TAG, "Send upload data at offset: " + offset);
        int dataLength = Math.min(mMtu - calculatePacketOverhead(mImageUploadData, offset),
                mImageUploadData.length - offset);
        Log.v(TAG, "Image data length: " + dataLength);
        byte[] sendBuffer = new byte[dataLength];
        for (int i = 0; i < dataLength; i++) {
            sendBuffer[i] = mImageUploadData[offset + i];
        }
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("data", sendBuffer);
        if (offset == 0) {
            payloadMap.put("len", mImageUploadData.length);
        }
        payloadMap.put("off", offset);
        send(OP_WRITE, ID_UPLOAD, payloadMap, McuMgrImageUploadResponse.class, mCallback);
    }

    private McuMgrCallback mCallback = new McuMgrCallback<McuMgrImageUploadResponse>() {
        @Override
        public void onResponse(McuMgrImageUploadResponse response) {
            if (!response.isSuccess()) {
                cancelUpload(new McuMgrException("Command failed!"));
                return;
            }
            if (response.rc != 0) {
                // TODO when the image in slot 1 is confirmed, this will return ENOMEM (2).
                Log.e(TAG, "Upload failed due to Newt Manager error: " + response.rc);
                cancelUpload(new McuMgrErrorException(McuMgrErrorCode.valueOf(response.rc)));
                return;
            }
            mUploadOffset = response.off;
            mUploadCallback.onProgressChange(mUploadOffset, mImageUploadData.length,
                    new Date());
            if (mUploadOffset == mImageUploadData.length) {
                Log.d(TAG, "Upload finished!");
                mUploadCallback.onUploadFinish();
                return;
            }
            sendUploadData(mUploadOffset);
        }

        @Override
        public void onError(McuMgrException error) {
            cancelUpload(error);
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
            if (getScheme() == McuMgrScheme.COAP_BLE || getScheme() == McuMgrScheme.COAP_UDP) {
                overheadTestMap.put("_h", nmgrHeader);
                byte[] cborData = CBOR.toBytes(overheadTestMap);
                // 20 byte estimate of CoAP Header; 5 bytes for good measure
                return cborData.length + 20 + 5;
            } else {
                byte[] cborData = CBOR.toBytes(overheadTestMap);
                // 8 bytes for McuMgrHeader; 5 bytes for good measure
                return cborData.length + 8 + 5;
            }
        } catch (IOException e) {
            e.printStackTrace();
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

    public static byte[] getHashFromImage(byte[] data) throws McuMgrException {
        McuMgrImageTlvParser parser = new McuMgrImageTlvParser(data);

        return parser.hash();
    }
}
