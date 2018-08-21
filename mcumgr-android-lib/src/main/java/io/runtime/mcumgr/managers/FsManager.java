/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.managers;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.HashMap;

import io.runtime.mcumgr.McuManager;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.InsufficientMtuException;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.fs.McuMgrFsDownloadResponse;
import io.runtime.mcumgr.response.fs.McuMgrFsUploadResponse;
import io.runtime.mcumgr.util.CBOR;
import timber.log.Timber;

@SuppressWarnings({"WeakerAccess", "unused"})
public class FsManager extends McuManager {

    private final static int ID_FILE = 0;

    /**
     * Construct a McuManager instance.
     *
     * @param transporter the transporter to use to send commands.
     */
    public FsManager(final McuMgrTransport transporter) {
        super(GROUP_FS, transporter);
    }

    /**
     * Read a packet of a file with given name from the specified offset from the device
     * (asynchronous).
     * <p>
     * Use {@link #download(String, FileDownloadCallback)} to download the whole file
     * asynchronously using one command.
     *
     * @param name   the file name.
     * @param offset the offset, from which the chunk will be requested.
     * @param callback the asynchronous callback.
     * @see #upload(String, byte[], FileUploadCallback)
     */
    public void download(@NonNull String name, int offset,
                         @NonNull McuMgrCallback<McuMgrFsDownloadResponse> callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("name", name);
        payloadMap.put("off", offset);
        send(OP_READ, ID_FILE, payloadMap, McuMgrFsDownloadResponse.class, callback);
    }

    /**
     * Read a packet of a file with given name from the specified offset from the device
     * (synchronous).
     * <p>
     * Use {@link #download(String, FileDownloadCallback)} to download the whole file
     * asynchronously using one command.
     *
     * @param name   the file name.
     * @param offset the offset, from which the chunk will be requested.
     * @return The upload response.
     * @see #upload(String, byte[], FileUploadCallback)
     */
    @NonNull
    public McuMgrFsDownloadResponse download(@NonNull String name, int offset)
            throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("name", name);
        payloadMap.put("off", offset);
        return send(OP_READ, ID_FILE, payloadMap, McuMgrFsDownloadResponse.class);
    }

    /**
     * Send a packet of given data from the specified offset to the device (asynchronous).
     * <p>
     * The chunk size is limited by the current MTU. If the current MTU set by
     * {@link #setUploadMtu(int)} is too large, the {@link InsufficientMtuException} error will be
     * thrown. Use {@link InsufficientMtuException#getMtu()} to get the current MTU and
     * pass it to {@link #setUploadMtu(int)} and try again.
     * <p>
     * Use {@link #upload(String, byte[], FileUploadCallback)} to send the whole file asynchronously
     * using one command.
     *
     * @param name   the file name.
     * @param data   the file data.
     * @param offset the offset, from which the chunk will be sent.
     * @param callback the asynchronous callback.
     * @see #upload(String, byte[], FileUploadCallback)
     */
    public void upload(@NonNull String name, @NonNull byte[] data, int offset,
                       @NonNull McuMgrCallback<McuMgrFsUploadResponse> callback) {
        // Get the length of data (in bytes) to put into the upload packet. This calculated as:
        // min(MTU - packetOverhead, imageLength - uploadOffset)
        int dataLength = Math.min(mMtu - calculatePacketOverhead(name, data, offset),
                data.length - offset);

        // Copy the data from the image into a buffer.
        byte[] sendBuffer = new byte[dataLength];
        System.arraycopy(data, offset, sendBuffer, 0, dataLength);

        // Create the map of key-values for the McuManager payload
        HashMap<String, Object> payloadMap = new HashMap<>();
        // Put the name, data and offset
        payloadMap.put("name", name);
        payloadMap.put("data", sendBuffer);
        payloadMap.put("off", offset);
        if (offset == 0) {
            // Only send the length of the image in the first packet of the upload
            payloadMap.put("len", data.length);
        }

        // Send the request
        send(OP_WRITE, ID_FILE, payloadMap, McuMgrFsUploadResponse.class, callback);
    }

    /**
     * Send a packet of given data from the specified offset to the device (synchronous).
     * <p>
     * The chunk size is limited by the current MTU. If the current MTU set by
     * {@link #setUploadMtu(int)} is too large, the {@link InsufficientMtuException} error will be
     * thrown. Use {@link InsufficientMtuException#getMtu()} to get the current MTU and
     * pass it to {@link #setUploadMtu(int)} and try again.
     * <p>
     * Use {@link #upload(String, byte[], FileUploadCallback)} to send the whole file asynchronously
     * using one command.
     *
     * @param name   the file name.
     * @param data   the file data.
     * @param offset the offset, from which the chunk will be sent.
     * @return The upload response.
     * @see #upload(String, byte[], FileUploadCallback)
     */
    @NonNull
    public McuMgrFsUploadResponse upload(@NonNull String name, @NonNull byte[] data, int offset)
            throws McuMgrException {
        // Get the length of data (in bytes) to put into the upload packet. This calculated as:
        // min(MTU - packetOverhead, imageLength - uploadOffset)
        int dataLength = Math.min(mMtu - calculatePacketOverhead(name, data, offset),
                data.length - offset);

        // Copy the data from the image into a buffer.
        byte[] sendBuffer = new byte[dataLength];
        System.arraycopy(data, offset, sendBuffer, 0, dataLength);

        // Create the map of key-values for the McuManager payload
        HashMap<String, Object> payloadMap = new HashMap<>();
        // Put the name, data and offset
        payloadMap.put("name", name);
        payloadMap.put("data", sendBuffer);
        payloadMap.put("off", offset);
        if (offset == 0) {
            // Only send the length of the image in the first packet of the upload
            payloadMap.put("len", data.length);
        }

        // Send the request
        return send(OP_WRITE, ID_FILE, payloadMap, McuMgrFsUploadResponse.class);
    }

    /**
     * Begin a file download.
     * <p>
     * Only one download can occur per FsManager.
     *
     * @param name     the file name.
     * @param callback the file download callback.
     */
    public synchronized void download(@NonNull String name, @NonNull FileDownloadCallback callback) {
        if (mTransferState == STATE_NONE) {
            mTransferState = STATE_DOWNLOADING;
        } else {
            Timber.d("FsManager is not ready");
            return;
        }

        mFileName = name;
        mDownloadCallback = callback;
        requestNext(0);
    }

    /**
     * Begin a file upload.
     * <p>
     * Only one upload can occur per FsManager.
     *
     * @param name     the file name.
     * @param data     the file data to upload.
     * @param callback the file upload callback.
     */
    public synchronized void upload(@NonNull String name, @NonNull byte[] data,
                       @NonNull FileUploadCallback callback) {
        if (mTransferState == STATE_NONE) {
            mTransferState = STATE_UPLOADING;
        } else {
            Timber.d("FsManager is not ready");
            return;
        }

        mFileName = name;
        mFileData = data;
        mUploadCallback = callback;
        sendNext(0);
    }

    //******************************************************************
    // File Upload / Download
    //******************************************************************

    // Upload / Download states
    public final static int STATE_NONE = 0;
    public final static int STATE_UPLOADING = 1;
    public final static int STATE_DOWNLOADING = 2;
    public final static int STATE_PAUSED = 3;

    // Upload / Download variables
    private int mTransferState = STATE_NONE;
    private String mFileName = null;
    private int mOffset = 0;
    private byte[] mFileData;
    private FileUploadCallback mUploadCallback;
    private FileDownloadCallback mDownloadCallback;

    /**
     * Get the current upload state ({@link #STATE_NONE},
     * {@link #STATE_UPLOADING}, {@link #STATE_DOWNLOADING}, {@link #STATE_PAUSED}).
     *
     * @return The current upload state.
     */
    public synchronized int getState() {
        return mTransferState;
    }

    /**
     * Cancel an undergoing file transfer. Does nothing if no transfer is in progress.
     */
    public synchronized void cancelTransfer() {
        if (mTransferState == STATE_NONE) {
            Timber.d("File transfer is not in progress");
        } else if (mTransferState == STATE_PAUSED) {
            Timber.d("Upload canceled!");
            resetTransfer();
            if (mUploadCallback != null) {
                mUploadCallback.onUploadCanceled();
                mUploadCallback = null;
            }
            if (mDownloadCallback != null) {
                mDownloadCallback.onDownloadCanceled();
                mDownloadCallback = null;
            }
        } else {
            // Transfer will be cancelled
            resetTransfer();
        }
    }

    /**
     * Pause an in progress transfer.
     */
    public synchronized void pauseTransfer() {
        if (mTransferState == STATE_NONE) {
            Timber.d("File transfer is not in progress.");
        } else {
            Timber.d("Upload paused.");
            mTransferState = STATE_PAUSED;
        }
    }

    /**
     * Continue a paused file transfer.
     */
    public synchronized void continueTransfer() {
        if (mTransferState == STATE_PAUSED) {
            Timber.d("Continuing transfer.");
            if (mDownloadCallback != null) {
                mTransferState = STATE_DOWNLOADING;
                requestNext(mOffset);
            } else {
                mTransferState = STATE_UPLOADING;
                sendNext(mOffset);
            }
        } else {
            Timber.d("Transfer is not paused.");
        }
    }

    //******************************************************************
    // Implementation
    //******************************************************************

    private synchronized void fail(McuMgrException error) {
        if (mUploadCallback != null) {
            mUploadCallback.onUploadFailed(error);
        }else if (mDownloadCallback != null) {
            mDownloadCallback.onDownloadFailed(error);
        }
        resetTransfer();
        mUploadCallback = null;
        mDownloadCallback = null;
    }

    private synchronized void restartTransfer() {
        mTransferState = STATE_NONE;
        if (mUploadCallback != null) {
            upload(mFileName, mFileData, mUploadCallback);
        } else if (mDownloadCallback != null) {
            download(mFileName, mDownloadCallback);
        }
    }

    private synchronized void resetTransfer() {
        mTransferState = STATE_NONE;
        mFileName = null;
        mOffset = 0;
        mFileData = null;
    }

    /**
     * Send a packet of upload data from the specified offset.
     *
     * @param offset the image data offset to send data from.
     */
    private synchronized void sendNext(int offset) {
        // Check that the state is STATE_UPLOADING
        if (mTransferState != STATE_UPLOADING) {
            Timber.d("Fs Manager is not in the UPLOADING state.");
            return;
        }
        upload(mFileName, mFileData, offset, mUploadCallbackImpl);
    }

    /**
     * Send a packet of upload data from the specified offset.
     *
     * @param offset the image data offset to send data from.
     */
    private synchronized void requestNext(int offset) {
        // Check that the state is STATE_UPLOADING
        if (mTransferState != STATE_DOWNLOADING) {
            Timber.d("Fs Manager is not in the DOWNLOADING state.");
            return;
        }
        download(mFileName, offset, mDownloadCallbackImpl);
    }

    /**
     * The upload callback which is called after a {@link #sendNext(int)}'s response has been
     * received or an error has occurred. On success, this callback parses the response, calls the
     * upload progress callback and sends the next packet of image data from the offset specified
     * in the response. On error, the upload is failed unless the error specifies that the packet
     * sent to the transporter was too large to send ({@link InsufficientMtuException}).
     * In this case, the MTU is set to the MTU in the exception and the upload is restarted.
     */
    private final McuMgrCallback<McuMgrFsUploadResponse> mUploadCallbackImpl =
            new McuMgrCallback<McuMgrFsUploadResponse>() {
                @Override
                public void onResponse(@NonNull McuMgrFsUploadResponse response) {
                    // Check for a McuManager error
                    if (response.rc != 0) {
                        Timber.e("Upload failed due to McuManager error: %s",  response.rc);
                        fail(new McuMgrErrorException(McuMgrErrorCode.valueOf(response.rc)));
                        return;
                    }

                    // Check if upload hasn't been cancelled.
                    if (mTransferState == STATE_NONE) {
                        Timber.d("Upload canceled!");
                        resetTransfer();
                        mUploadCallback.onUploadCanceled();
                        mUploadCallback = null;
                        return;
                    }

                    // Get the next offset to send image data from.
                    mOffset = response.off;

                    // Call the progress callback.
                    mUploadCallback.onProgressChanged(mOffset, mFileData.length,
                            System.currentTimeMillis());

                    // Check if the upload has finished.
                    if (mOffset == mFileData.length) {
                        Timber.d("Upload finished!");
                        resetTransfer();
                        mUploadCallback.onUploadFinished();
                        mUploadCallback = null;
                        return;
                    }

                    // Send the next packet of upload data from the offset provided in the response.
                    sendNext(mOffset);
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
                            restartTransfer();
                            return;
                        }
                    }
                    // If the exception is not due to insufficient MTU fail the upload.
                    fail(error);
                }
            };

    /**
     * The download callback which is called after a {@link #requestNext(int)}'s response has been
     * received or an error has occurred. On success, this callback parses the response, calls the
     * download progress callback and requests the next packet of file data from the offset specified
     * in the response. On error, the transfer is failed.
     */
    private final McuMgrCallback<McuMgrFsDownloadResponse> mDownloadCallbackImpl =
            new McuMgrCallback<McuMgrFsDownloadResponse>() {
                @Override
                public void onResponse(@NonNull McuMgrFsDownloadResponse response) {
                    // Check for a McuManager error.
                    if (response.rc != 0) {
                        Timber.e("Download failed due to McuManager error: %s", response.rc);
                        fail(new McuMgrErrorException(McuMgrErrorCode.valueOf(response.rc)));
                        return;
                    }

                    // Check if download hasn't been cancelled.
                    if (mTransferState == STATE_NONE) {
                        Timber.d("Download canceled!");
                        resetTransfer();
                        mDownloadCallback.onDownloadCanceled();
                        mDownloadCallback = null;
                        return;
                    }

                    // Get the next offset to request data from.
                    mOffset = response.off;

                    // The first packet contains the file length.
                    if (mOffset == 0) {
                        mFileData = new byte[response.len];
                    }

                    // Copy received data to the buffer.
                    System.arraycopy(response.data, 0, mFileData, mOffset, response.data.length);
                    mOffset += response.data.length;

                    // Call the progress callback.
                    mDownloadCallback.onProgressChanged(mOffset, mFileData.length,
                            System.currentTimeMillis());

                    // Check if the download has finished.
                    if (mOffset == mFileData.length) {
                        Timber.d("Download finished!");
                        byte[] data = mFileData;
                        String fileName = mFileName;
                        resetTransfer();
                        mDownloadCallback.onDownloadFinished(fileName, data);
                        mDownloadCallback = null;
                        return;
                    }

                    // Send the next packet of upload data from the offset provided in the response.
                    requestNext(mOffset);
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
                            restartTransfer();
                            return;
                        }
                    }
                    // If the exception is not due to insufficient MTU fail the upload.
                    fail(error);
                }
            };

    // TODO more precise overhead calculations
    private int calculatePacketOverhead(@NonNull String name, @NonNull byte[] data, int offset) {
        HashMap<String, Object> overheadTestMap = new HashMap<>();
        overheadTestMap.put("name", name);
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
            Timber.e(e, "Error while calculating packet overhead");
        }
        return -1;
    }

    //******************************************************************
    // File Upload Callback
    //******************************************************************

    public interface FileUploadCallback {

        /**
         * Called when a response has been received successfully.
         *
         * @param bytesSent the number of bytes sent so far.
         * @param imageSize the size of the image in bytes.
         * @param timestamp the timestamp of when the response was received.
         */
        void onProgressChanged(int bytesSent, int imageSize, long timestamp);

        /**
         * Called when the upload has failed.
         *
         * @param error the error. See the cause for more info.
         */
        void onUploadFailed(McuMgrException error);

        /**
         * Called when the upload has been canceled.
         */
        void onUploadCanceled();

        /**
         * Called when the upload has finished successfully.
         */
        void onUploadFinished();
    }

    //******************************************************************
    // File Download Callback
    //******************************************************************

    public interface FileDownloadCallback {

        /**
         * Called when a response has been received successfully.
         *
         * @param bytesDownloaded the number of bytes downloaded so far.
         * @param imageSize       the size of the image in bytes.
         * @param timestamp       the timestamp of when the response was received.
         */
        void onProgressChanged(int bytesDownloaded, int imageSize, long timestamp);

        /**
         * Called when the download has failed.
         *
         * @param error the error. See the cause for more info.
         */
        void onDownloadFailed(@NonNull McuMgrException error);

        /**
         * Called when the download has been canceled.
         */
        void onDownloadCanceled();

        /**
         * Called when the download has finished successfully.
         *
         * @param name file name.
         * @param data file data.
         */
        void onDownloadFinished(@NonNull String name, @NonNull byte[] data);
    }
}
