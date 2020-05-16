/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.managers;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.InsufficientMtuException;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.DownloadResponse;
import io.runtime.mcumgr.response.UploadResponse;
import io.runtime.mcumgr.response.fs.McuMgrFsDownloadResponse;
import io.runtime.mcumgr.response.fs.McuMgrFsUploadResponse;
import io.runtime.mcumgr.transfer.Download;
import io.runtime.mcumgr.transfer.DownloadCallback;
import io.runtime.mcumgr.transfer.TransferController;
import io.runtime.mcumgr.transfer.TransferManager;
import io.runtime.mcumgr.transfer.Upload;
import io.runtime.mcumgr.transfer.UploadCallback;
import io.runtime.mcumgr.util.CBOR;

@SuppressWarnings({"WeakerAccess", "unused"})
public class FsManager extends TransferManager {

    private final static Logger LOG = LoggerFactory.getLogger(FsManager.class);

    private final static int ID_FILE = 0;

    /**
     * Construct a McuManager instance.
     *
     * @param transporter the transporter to use to send commands.
     */
    public FsManager(@NotNull McuMgrTransport transporter) {
        super(GROUP_FS, transporter);
    }

    /**
     * Read a packet of a file with given name from the specified offset from the device
     * (asynchronous).
     * <p>
     * Use {@link #fileDownload} to download the whole file asynchronously using one command.
     *
     * @param name   the file name.
     * @param offset the offset, from which the chunk will be requested.
     * @param callback the asynchronous callback.
     * @see #fileDownload(String, DownloadCallback)
     */
    public void download(@NotNull String name, int offset,
                         @NotNull McuMgrCallback<McuMgrFsDownloadResponse> callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("name", name);
        payloadMap.put("off", offset);
        send(OP_READ, ID_FILE, payloadMap, McuMgrFsDownloadResponse.class, callback);
    }

    /**
     * Read a packet of a file with given name from the specified offset from the device
     * (synchronous).
     * <p>
     * Use {@link #fileDownload} to download the whole file asynchronously using one command.
     *
     * @param name   the file name.
     * @param offset the offset, from which the chunk will be requested.
     * @return The upload response.
     * @see #fileDownload(String, DownloadCallback)
     */
    @NotNull
    public McuMgrFsDownloadResponse download(@NotNull String name, int offset)
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
     * Use {@link #fileUpload} to upload the whole file asynchronously using one command.
     *
     * @param name   the file name.
     * @param data   the file data.
     * @param offset the offset, from which the chunk will be sent.
     * @param callback the asynchronous callback.
     * @see #fileUpload(String, byte[], UploadCallback)
     */
    public void upload(@NotNull String name, @NotNull byte[] data, int offset,
                       @NotNull McuMgrCallback<McuMgrFsUploadResponse> callback) {
        HashMap<String, Object> payloadMap = buildUploadPayload(name, data, offset);
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
     * Use {@link #fileUpload} to upload the whole file asynchronously using one command.
     *
     * @param name   the file name.
     * @param data   the file data.
     * @param offset the offset, from which the chunk will be sent.
     * @return The upload response.
     * @see #fileUpload(String, byte[], UploadCallback)
     */
    @NotNull
    public McuMgrFsUploadResponse upload(@NotNull String name, @NotNull byte[] data, int offset)
            throws McuMgrException {
        HashMap<String, Object> payloadMap = buildUploadPayload(name, data, offset);
        return send(OP_WRITE, ID_FILE, payloadMap, McuMgrFsUploadResponse.class);
    }

    /*
     * Build the upload payload map.
     */
    @NotNull
    private HashMap<String, Object> buildUploadPayload(@NotNull String name, @NotNull byte[] data, int offset) {
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
        return payloadMap;
    }

    //******************************************************************
    // File Upload
    //******************************************************************

    /**
     * Start image upload.
     * <p>
     * Multiple calls will queue multiple uploads, executed sequentially. This includes file
     * downloads executed from {@link #fileDownload}.
     * <p>
     * The upload may be controlled using the {@link TransferController} returned by this method.
     *
     * @param data The file data to upload.
     * @param callback  Receives callbacks from the upload.
     * @return The object used to control this upload.
     * @see TransferController
     */
    @NotNull
    public TransferController fileUpload(@NotNull String name, @NotNull byte[] data, @NotNull UploadCallback callback) {
        return startUpload(new FileUpload(name, data, callback));
    }

    /**
     * File Upload Implementation.
     */
    public class FileUpload extends Upload {

        @NotNull
        private String mName;

        protected FileUpload(@NotNull String name, @NotNull byte[] data, @NotNull UploadCallback callback) {
            super(data, callback);
            mName = name;
        }

        @Override
        protected UploadResponse write(@NotNull byte[] data, int offset) throws McuMgrException {
            return upload(mName, data, offset);
        }
    }

    //******************************************************************
    // File Download
    //******************************************************************

    /**
     * Start image upload.
     * <p>
     * Multiple calls will queue multiple uploads, executed sequentially. This includes file
     * downloads executed from {@link #fileUpload}.
     * <p>
     * The upload may be controlled using the {@link TransferController} returned by this method.
     *
     * @param callback Receives callbacks from the upload.
     * @return The object used to control this upload.
     * @see TransferController
     */
    @NotNull
    public TransferController fileDownload(@NotNull String name, @NotNull DownloadCallback callback) {
        return startDownload(new FileDownload(name, callback));
    }

    /**
     * Start image upload.
     * <p>
     * Multiple calls will queue multiple uploads, executed sequentially. This includes file
     * downloads executed from {@link #fileUpload}.
     * <p>
     * The upload may be controlled using the {@link TransferController} returned by this method.
     *
     * @param callback Receives callbacks from the upload.
     * @return The object used to control this upload.
     * @see TransferController
     * @deprecated Use {@link #fileDownload(String, DownloadCallback)} instead.
     */
    @Deprecated
    @NotNull
    public TransferController fileDownload(@NotNull String name, @NotNull byte[] data, @NotNull DownloadCallback callback) {
        return fileDownload(name, callback);
    }

    /**
     * File Download Implementation
     */
    public class FileDownload extends Download {

        @NotNull
        private String mName;

        protected FileDownload(@NotNull String name, @NotNull DownloadCallback callback) {
            super(callback);
            mName = name;
        }

        @Override
        protected DownloadResponse read(int offset) throws McuMgrException {
            return download(mName, offset);
        }
    }

    //******************************************************************
    // File Upload / Download (OLD, DEPRECATED)
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
     * Begin a file download.
     * <p>
     * Only one download can occur per FsManager.
     *
     * @param name     the file name.
     * @param callback the file download callback.
     * @deprecated Use {@link #fileDownload(String, DownloadCallback)} instead.
     */
    @Deprecated
    public synchronized void download(@NotNull String name, @NotNull FileDownloadCallback callback) {
        if (mTransferState == STATE_NONE) {
            mTransferState = STATE_DOWNLOADING;
        } else {
            LOG.debug("FsManager is not ready");
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
     * @deprecated Use {@link #fileUpload(String, byte[], UploadCallback)} instead.
     */
    @Deprecated
    public synchronized void upload(@NotNull String name, @NotNull byte[] data,
                                    @NotNull FileUploadCallback callback) {
        if (mTransferState == STATE_NONE) {
            mTransferState = STATE_UPLOADING;
        } else {
            LOG.debug("FsManager is not ready");
            return;
        }

        mFileName = name;
        mFileData = data;
        mUploadCallback = callback;
        sendNext(0);
    }

    /**
     * Get the current upload state ({@link #STATE_NONE},
     * {@link #STATE_UPLOADING}, {@link #STATE_DOWNLOADING}, {@link #STATE_PAUSED}).
     *
     * @return The current upload state.
     * @deprecated Old implementation. See {@link #fileUpload} and {@link #fileDownload}
     */
    @Deprecated
    public synchronized int getState() {
        return mTransferState;
    }

    /**
     * Cancel an undergoing file transfer. Does nothing if no transfer is in progress.
     * @deprecated Old implementation. See {@link #fileUpload} and {@link #fileDownload}
     */
    @Deprecated
    public synchronized void cancelTransfer() {
        if (mTransferState == STATE_NONE) {
            LOG.debug("File transfer is not in progress");
        } else if (mTransferState == STATE_PAUSED) {
            LOG.debug("Upload canceled!");
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
     * @deprecated Old implementation. See {@link #fileUpload} and {@link #fileDownload}
     */
    @Deprecated
    public synchronized void pauseTransfer() {
        if (mTransferState == STATE_NONE) {
            LOG.debug("File transfer is not in progress.");
        } else {
            LOG.debug("Upload paused.");
            mTransferState = STATE_PAUSED;
        }
    }

    /**
     * Continue a paused file transfer.
     * @deprecated Old implementation. See {@link #fileUpload} and {@link #fileDownload}
     */
    @Deprecated
    public synchronized void continueTransfer() {
        if (mTransferState == STATE_PAUSED) {
            LOG.debug("Continuing transfer.");
            if (mDownloadCallback != null) {
                mTransferState = STATE_DOWNLOADING;
                requestNext(mOffset);
            } else {
                mTransferState = STATE_UPLOADING;
                sendNext(mOffset);
            }
        } else {
            LOG.debug("Transfer is not paused.");
        }
    }

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
            LOG.debug("Fs Manager is not in the UPLOADING state.");
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
            LOG.debug("Fs Manager is not in the DOWNLOADING state.");
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
                public void onResponse(@NotNull McuMgrFsUploadResponse response) {
                    // Check for a McuManager error
                    if (response.rc != 0) {
                        LOG.error("Upload failed due to McuManager error: {}",  response.rc);
                        fail(new McuMgrErrorException(McuMgrErrorCode.valueOf(response.rc)));
                        return;
                    }

                    // Check if upload hasn't been cancelled.
                    if (mTransferState == STATE_NONE) {
                        LOG.debug("Upload canceled!");
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
                        LOG.debug("Upload finished!");
                        resetTransfer();
                        mUploadCallback.onUploadFinished();
                        mUploadCallback = null;
                        return;
                    }

                    // Send the next packet of upload data from the offset provided in the response.
                    sendNext(mOffset);
                }

                @Override
                public void onError(@NotNull McuMgrException error) {
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
                public void onResponse(@NotNull McuMgrFsDownloadResponse response) {
                    // Check for a McuManager error.
                    if (response.rc != 0) {
                        LOG.error("Download failed due to McuManager error: {}", response.rc);
                        fail(new McuMgrErrorException(McuMgrErrorCode.valueOf(response.rc)));
                        return;
                    }

                    // Check if download hasn't been cancelled.
                    if (mTransferState == STATE_NONE) {
                        LOG.debug("Download canceled!");
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
                        LOG.debug("Download finished!");
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
                public void onError(@NotNull McuMgrException error) {
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
    private int calculatePacketOverhead(@NotNull String name, @NotNull byte[] data, int offset) {
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
                // 8 bytes for McuMgr header; 2 bytes for data length
                return cborData.length + 8 + 2;
            }
        } catch (IOException e) {
            LOG.error("Error while calculating packet overhead", e);
        }
        return -1;
    }

    //******************************************************************
    // File Upload Callback
    //******************************************************************

    /**
     * @deprecated Old implementation. See {@link #fileUpload} and {@link #fileDownload}
     */
    @Deprecated
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

    /**
     * @deprecated Old implementation. See {@link #fileUpload} and {@link #fileDownload}
     */
    @Deprecated
    public interface FileDownloadCallback {

        /**
         * Called when a response has been received successfully.
         *
         * @param bytesDownloaded the number of bytes downloaded so far.
         * @param imageSize       the size of the image in bytes.
         * @param timestamp       the timestamp of when the response was received.
         * @deprecated Old implementation. See {@link #fileUpload} and {@link #fileDownload}
         */
        void onProgressChanged(int bytesDownloaded, int imageSize, long timestamp);

        /**
         * Called when the download has failed.
         *
         * @param error the error. See the cause for more info.
         * @deprecated Old implementation. See {@link #fileUpload} and {@link #fileDownload}
         */
        void onDownloadFailed(@NotNull McuMgrException error);

        /**
         * Called when the download has been canceled.
         * @deprecated Old implementation. See {@link #fileUpload} and {@link #fileDownload}
         */
        void onDownloadCanceled();

        /**
         * Called when the download has finished successfully.
         *
         * @param name file name.
         * @param data file data.
         * @deprecated Old implementation. See {@link #fileUpload} and {@link #fileDownload}
         */
        void onDownloadFinished(@NotNull String name, @NotNull byte[] data);
    }
}
