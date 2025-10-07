/*
 * Copyright (c) 2017-2018 Runtime Inc.
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.managers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.McuMgrGroupReturnCode;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.crash.CoreDump;
import io.runtime.mcumgr.dfu.mcuboot.FirmwareUpgradeManager;
import io.runtime.mcumgr.dfu.mcuboot.model.TargetImage;
import io.runtime.mcumgr.exception.InsufficientMtuException;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.DownloadResponse;
import io.runtime.mcumgr.response.HasReturnCode;
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.response.UploadResponse;
import io.runtime.mcumgr.response.img.McuMgrCoreLoadResponse;
import io.runtime.mcumgr.response.img.McuMgrImageResponse;
import io.runtime.mcumgr.response.img.McuMgrImageSlotResponse;
import io.runtime.mcumgr.response.img.McuMgrImageStateResponse;
import io.runtime.mcumgr.response.img.McuMgrImageUploadResponse;
import io.runtime.mcumgr.transfer.Download;
import io.runtime.mcumgr.transfer.DownloadCallback;
import io.runtime.mcumgr.transfer.TransferController;
import io.runtime.mcumgr.transfer.TransferManager;
import io.runtime.mcumgr.transfer.Upload;
import io.runtime.mcumgr.transfer.UploadCallback;
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
public class ImageManager extends TransferManager {

    public enum ReturnCode implements McuMgrGroupReturnCode {
        /** No error, this is implied if there is no ret value in the response */
        OK(0),

        /** Unknown error occurred. */
        UNKNOWN(1),

        /** Failed to query flash area configuration. */
        FLASH_CONFIG_QUERY_FAIL(2),

        /** There is no image in the slot. */
        NO_IMAGE(3),

        /** The image in the slot has no TLVs (tag, length, value). */
        NO_TLVS(4),

        /** The image in the slot has an invalid TLV type and/or length. */
        INVALID_TLV(5),

        /** The image in the slot has multiple hash TLVs, which is invalid. */
        TLV_MULTIPLE_HASHES_FOUND(6),

        /** The image in the slot has an invalid TLV size. */
        TLV_INVALID_SIZE(7),

        /** The image in the slot does not have a hash TLV, which is required.  */
        HASH_NOT_FOUND(8),

        /** There is no free slot to place the image. */
        NO_FREE_SLOT(9),

        /** Flash area opening failed. */
        FLASH_OPEN_FAILED(10),

        /** Flash area reading failed. */
        FLASH_READ_FAILED(11),

        /** Flash area writing failed. */
        FLASH_WRITE_FAILED(12),

        /** Flash area erase failed. */
        FLASH_ERASE_FAILED(13),

        /** The provided slot is not valid. */
        INVALID_SLOT(14),

        /** Insufficient heap memory (malloc failed). */
        NO_FREE_MEMORY(15),

        /** The flash context is already set. */
        FLASH_CONTEXT_ALREADY_SET(16),

        /** The flash context is not set. */
        FLASH_CONTEXT_NOT_SET(17),

        /** The device for the flash area is NULL. */
        FLASH_AREA_DEVICE_NULL(18),

        /** The offset for a page number is invalid. */
        INVALID_PAGE_OFFSET(19),

        /** The offset parameter was not provided and is required. */
        INVALID_OFFSET(20),

        /** The length parameter was not provided and is required. */
        INVALID_LENGTH(21),

        /** The image length is smaller than the size of an image header. */
        INVALID_IMAGE_HEADER(22),

        /** The image header magic value does not match the expected value. */
        INVALID_IMAGE_HEADER_MAGIC(23),

        /** The hash parameter provided is not valid. */
        INVALID_HASH(24),

        /** The image load address does not match the address of the flash area. */
        INVALID_FLASH_ADDRESS(25),

        /** Failed to get version of currently running application. */
        VERSION_GET_FAILED(26),

        /** The currently running application is newer than the version being uploaded. */
        CURRENT_VERSION_IS_NEWER(27),

        /** There is already an image operating pending. */
        IMAGE_ALREADY_PENDING(28),

        /** The image vector table is invalid. */
        INVALID_IMAGE_VECTOR_TABLE(29),
        /** The image it too large to fit. */
        INVALID_IMAGE_TOO_LARGE(30),

        /** The amount of data sent is larger than the provided image size. */
        INVALID_IMAGE_DATA_OVERRUN(31),

        /** Confirmation of image has been denied */
        IMAGE_CONFIRMATION_DENIED(32),

        /** Setting test to active slot is not allowed */
        IMAGE_SETTING_TEST_TO_ACTIVE_DENIED(33),

        /** Current active slot for image cannot be determined */
        ACTIVE_SLOT_NOT_KNOWN(34);

        private final int mCode;

        ReturnCode(int code) {
            mCode = code;
        }

        public int value() {
            return mCode;
        }

        public static @Nullable ImageManager.ReturnCode valueOf(@Nullable McuMgrResponse.GroupReturnCode returnCode) {
            if (returnCode == null || returnCode.group != GROUP_IMAGE) {
                return null;
            }
            for (ImageManager.ReturnCode code : values()) {
                if (code.value() == returnCode.rc) {
                    return code;
                }
            }
            return UNKNOWN;
        }
    }

    public interface Response extends HasReturnCode {

        @Nullable
        default ImageManager.ReturnCode getImageReturnCode() {
            McuMgrResponse.GroupReturnCode groupReturnCode = getGroupReturnCode();
            if (groupReturnCode == null) {
                if (getReturnCodeValue() == McuMgrErrorCode.OK.value()) {
                    return ImageManager.ReturnCode.OK;
                }
                return ImageManager.ReturnCode.UNKNOWN;
            }
            return ImageManager.ReturnCode.valueOf(groupReturnCode);
        }
    }

    private final static Logger LOG = LoggerFactory.getLogger(ImageManager.class);

    private final static int IMG_HASH_LEN = 32;

    // Image manager command IDs
    private final static int ID_STATE = 0;
    private final static int ID_UPLOAD = 1;
    private final static int ID_FILE = 2;
    private final static int ID_CORELIST = 3;
    private final static int ID_CORELOAD = 4;
    private final static int ID_ERASE = 5;
    private final static int ID_SLOT_INFO = 6;

    /**
     * Construct an image manager.
     *
     * @param transport the transport to use to send commands.
     */
    public ImageManager(@NotNull McuMgrTransport transport) {
        super(GROUP_IMAGE, transport);
    }

    /**
     * List the images on a device (asynchronous).
     * <p>
     * The response payload can be mapped to a {@link McuMgrImageStateResponse}.
     *
     * @param callback the asynchronous callback.
     */
    public void list(@NotNull McuMgrCallback<McuMgrImageStateResponse> callback) {
        send(OP_READ, ID_STATE, null, MEDIUM_TIMEOUT, McuMgrImageStateResponse.class, callback);
    }

    /**
     * List the images on a device (synchronous).
     * <p>
     * The response payload can be mapped to a {@link McuMgrImageStateResponse}.
     *
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrImageStateResponse list() throws McuMgrException {
        return send(OP_READ, ID_STATE, null, MEDIUM_TIMEOUT, McuMgrImageStateResponse.class);
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
     * Use {@link #imageUpload(byte[], UploadCallback)} to send the whole file asynchronously
     * using one command.
     *
     * @param data     image data.
     * @param offset   the offset, from which the chunk will be sent.
     * @param callback the asynchronous callback.
     * @see #imageUpload(byte[], UploadCallback)
     */
    public void upload(byte @NotNull [] data, int offset,
                       @NotNull McuMgrCallback<McuMgrImageUploadResponse> callback) {
        upload(data, offset, 0, callback);
    }

    /**
     * Send a packet of given data from the specified offset to the given core (image) on the
     * device (asynchronous).
     * <p>
     * The chunk size is limited by the current MTU. If the current MTU set by
     * {@link #setUploadMtu(int)} is too large, the {@link McuMgrCallback#onError(McuMgrException)}
     * with {@link InsufficientMtuException} error will be returned.
     * Use {@link InsufficientMtuException#getMtu()} to get the current MTU and
     * pass it to {@link #setUploadMtu(int)} and try again.
     * <p>
     * Use {@link #imageUpload(byte[], UploadCallback)} to send the whole file asynchronously
     * using one command.
     *
     * @param data     image data.
     * @param offset   the offset, from which the chunk will be sent.
     * @param image    the image number, default is 0. Use 0 for core0, 1 for core1, etc.
     * @param callback the asynchronous callback.
     * @see #imageUpload(byte[], UploadCallback)
     */
    public void upload(byte @NotNull [] data, int offset, int image,
                       @NotNull McuMgrCallback<McuMgrImageUploadResponse> callback) {
        HashMap<String, Object> payloadMap = buildUploadPayload(data, offset, image);
        // Timeout for the initial chunk is long, as the device may need to erase the flash.
        final long timeout = offset == 0 ? DEFAULT_TIMEOUT : SHORT_TIMEOUT;
        send(OP_WRITE, ID_UPLOAD, payloadMap, timeout, McuMgrImageUploadResponse.class, callback);
    }

    /**
     * Send a packet of given data from the specified offset to the device (synchronous).
     * <p>
     * The chunk size is limited by the current MTU. If the current MTU set by
     * {@link #setUploadMtu(int)} is too large, the {@link InsufficientMtuException} error will be
     * thrown. Use {@link InsufficientMtuException#getMtu()} to get the current MTU and
     * pass it to {@link #setUploadMtu(int)} and try again.
     * <p>
     * Use {@link #imageUpload(byte[], UploadCallback)} to send the whole file asynchronously
     * using one command.
     *
     * @param data   image data.
     * @param offset the offset, from which the chunk will be sent.
     * @return The upload response.
     * @see #imageUpload(byte[], UploadCallback)
     */
    @NotNull
    public McuMgrImageUploadResponse upload(byte @NotNull [] data, int offset) throws McuMgrException {
        return upload(data, offset, 0);
    }

    /**
     * Send a packet of given data from the specified offset to the given core (image) on the
     * device (synchronous).
     * <p>
     * The chunk size is limited by the current MTU. If the current MTU set by
     * {@link #setUploadMtu(int)} is too large, the {@link InsufficientMtuException} error will be
     * thrown. Use {@link InsufficientMtuException#getMtu()} to get the current MTU and
     * pass it to {@link #setUploadMtu(int)} and try again.
     * <p>
     * Use {@link #imageUpload(byte[], UploadCallback)} to send the whole file asynchronously
     * using one command.
     *
     * @param data   image data.
     * @param offset the offset, from which the chunk will be sent.
     * @param image    the image number, default is 0. Use 0 for core0, 1 for core1, etc.
     * @return The upload response.
     * @see #imageUpload(byte[], UploadCallback)
     */
    @NotNull
    public McuMgrImageUploadResponse upload(byte @NotNull [] data, int offset, int image)
            throws McuMgrException {
        HashMap<String, Object> payloadMap = buildUploadPayload(data, offset, image);
        // Timeout for the initial chunk is long, as the device may need to erase the flash.
        final long timeout = offset == 0 ? DEFAULT_TIMEOUT : SHORT_TIMEOUT;
        return send(OP_WRITE, ID_UPLOAD, payloadMap, timeout, McuMgrImageUploadResponse.class);
    }

    /*
     * Build the upload payload.
     */
    @NotNull
    private HashMap<String, Object> buildUploadPayload(byte @NotNull [] data, int offset, int image) {
        // Get chunk of image data to send
        int overhead = calculatePacketOverhead(data, offset, image);
        int dataLength = Math.min(mMtu - overhead, data.length - offset);
        if (dataLength < 0) {
            throw new NegativeArraySizeException("Data length is negative (" + dataLength + " bytes)" +
                    " (MTU = " + mMtu +
                    ", CBOR overhead = " + overhead + ", data length = " + data.length +
                    ", offset = " + offset + ")");
        }
        byte[] sendBuffer = new byte[dataLength];
        System.arraycopy(data, offset, sendBuffer, 0, dataLength);

        // Create the map of key-values for the McuManager payload
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("data", sendBuffer);
        payloadMap.put("off", offset);
        if (offset == 0) {
            // Only send the length and image of the image in the first packet of the upload
            if (image > 0) {
                // Image 0 does not need to be sent, as it's default.
                payloadMap.put("image", image);
            }
            payloadMap.put("len", data.length);

            /*
             * Feature in Apache Mynewt: Device keeps track of unfinished uploads based on the
             * SHA256 hash over the image data. When an upload request is received which contains
             * the same hash of a partially finished upload, the device will send the offset to
             * continue from.
             */
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(data);
                payloadMap.put("sha", hash);
            } catch (NoSuchAlgorithmException e) {
                LOG.error("SHA-256 not found", e);
            }
        }
        return payloadMap;
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
    public void test(byte @NotNull [] hash, @NotNull McuMgrCallback<McuMgrImageStateResponse> callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("hash", hash);
        payloadMap.put("confirm", false);
        send(OP_WRITE, ID_STATE, payloadMap, SHORT_TIMEOUT, McuMgrImageStateResponse.class, callback);
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
    @NotNull
    public McuMgrImageStateResponse test(byte @NotNull [] hash) throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("hash", hash);
        payloadMap.put("confirm", false);
        return send(OP_WRITE, ID_STATE, payloadMap, SHORT_TIMEOUT, McuMgrImageStateResponse.class);
    }

    /**
     * Confirm an image on the device (asynchronous).
     * <p>
     * Confirming an image will make it the default to boot into.
     *
     * @param hash     the hash of the image to confirm.
     *                 If not provided, the current image running on the device will be made
     *                 permanent.
     * @param callback the asynchronous callback.
     */
    public void confirm(byte @Nullable [] hash, @NotNull McuMgrCallback<McuMgrImageStateResponse> callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("confirm", true);
        if (hash != null) {
            payloadMap.put("hash", hash);
        }
        send(OP_WRITE, ID_STATE, payloadMap, SHORT_TIMEOUT, McuMgrImageStateResponse.class, callback);
    }

    /**
     * Confirm an image on the device (synchronous).
     * <p>
     * Confirming an image will make it the default to boot into.
     *
     * @param hash the hash of the image to confirm.
     *             If not provided, the current image running on the device will be made
     *             permanent.
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrImageStateResponse confirm(byte @Nullable [] hash) throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("confirm", true);
        if (hash != null) {
            payloadMap.put("hash", hash);
        }
        return send(OP_WRITE, ID_STATE, payloadMap, SHORT_TIMEOUT, McuMgrImageStateResponse.class);
    }

    /**
     * Erase the secondary slot of the main image (asynchronous).
     *
     * @param callback the asynchronous callback.
     */
    public void erase(@NotNull McuMgrCallback<McuMgrImageResponse> callback) {
        erase(TargetImage.SLOT_SECONDARY, callback);
    }

    /**
     * Erase the given slot of the main image (asynchronous).
     *
     * @param slot    the slot id: 0 for primary, 1 for secondary (default).
     * @param callback the asynchronous callback.
     */
    public void erase(int slot, @NotNull McuMgrCallback<McuMgrImageResponse> callback) {
        HashMap<String, Object> payloadMap = null;
        // By default, the "opposite" slot to the currently running one is erased.
        // See: https://github.com/nrfconnect/sdk-zephyr/blob/f7859899ec7dbb21e0580eef25b229bda727f04a/subsys/mgmt/mcumgr/grp/img_mgmt/src/img_mgmt.c#L450
        if (slot != TargetImage.SLOT_SECONDARY) {
            payloadMap = new HashMap<>();
            payloadMap.put("slot", slot);
        }
        send(OP_WRITE, ID_ERASE, payloadMap, DEFAULT_TIMEOUT, McuMgrImageResponse.class, callback);
    }

    /**
     * Erase the secondary slot of the main image (synchronous).
     *
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrImageResponse erase() throws McuMgrException {
        return erase(TargetImage.SLOT_SECONDARY);
    }

    /**
     * Erase the given slot of the main image (synchronous).
     *
     * @param slot the slot id: 0 for primary, 1 for secondary (default).
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrImageResponse erase(int slot) throws McuMgrException {
        HashMap<String, Object> payloadMap = null;
        // By default, the "opposite" slot to the currently running one is erased.
        // See: https://github.com/nrfconnect/sdk-zephyr/blob/f7859899ec7dbb21e0580eef25b229bda727f04a/subsys/mgmt/mcumgr/grp/img_mgmt/src/img_mgmt.c#L450
        if (slot != TargetImage.SLOT_SECONDARY) {
            payloadMap = new HashMap<>();
            payloadMap.put("slot", slot);
        }
        return send(OP_WRITE, ID_ERASE, payloadMap, DEFAULT_TIMEOUT, McuMgrImageResponse.class);
    }

    /**
     * Reads slot information (asynchronous).
     *
     * @param callback the asynchronous callback.
     */
    public void slots(@NotNull McuMgrCallback<McuMgrImageSlotResponse> callback) {
        send(OP_READ, ID_SLOT_INFO, null, SHORT_TIMEOUT, McuMgrImageSlotResponse.class, callback);
    }

    /**
     * Reads slot information (synchronous).
     *
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrImageSlotResponse slots() throws McuMgrException {
        return send(OP_READ, ID_SLOT_INFO, null, SHORT_TIMEOUT, McuMgrImageSlotResponse.class);
    }

    /**
     * Core list (asynchronous).
     * <p>
     * A core dump is available for download if the {@link McuMgrErrorCode} is
     * {@link McuMgrErrorCode#OK}. If no core is available for download, the response will contain
     * a return code of {@link McuMgrErrorCode#NO_ENTRY}.
     *
     * @param callback the asynchronous callback.
     */
    public void coreList(@NotNull McuMgrCallback<McuMgrImageResponse> callback) {
        send(OP_READ, ID_CORELIST, null, SHORT_TIMEOUT, McuMgrImageResponse.class, callback);
    }

    /**
     * Core list (synchronous).
     * <p>
     * A core dump is available for download if the {@link McuMgrErrorCode} is
     * {@link McuMgrErrorCode#OK}. If no core is available for download, the response will contain
     * a return code of {@link McuMgrErrorCode#NO_ENTRY}.
     *
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrImageResponse coreList() throws McuMgrException {
        return send(OP_READ, ID_CORELIST, null, SHORT_TIMEOUT, McuMgrImageResponse.class);
    }

    /**
     * Core load (asynchronous).
     *
     * @param offset   offset.
     * @param callback the asynchronous callback.
     */
    public void coreLoad(int offset, @NotNull McuMgrCallback<McuMgrCoreLoadResponse> callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("off", offset);
        send(OP_READ, ID_CORELOAD, payloadMap, SHORT_TIMEOUT, McuMgrCoreLoadResponse.class, callback);
    }

    /**
     * Core load (synchronous).
     *
     * @param offset offset.
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrCoreLoadResponse coreLoad(int offset) throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("off", offset);
        return send(OP_READ, ID_CORELOAD, payloadMap, SHORT_TIMEOUT, McuMgrCoreLoadResponse.class);
    }

    /**
     * Erase a core dump from the device (asynchronous).
     *
     * @param callback the asynchronous callback.
     */
    public void coreErase(@NotNull McuMgrCallback<McuMgrImageResponse> callback) {
        send(OP_WRITE, ID_CORELOAD, null, DEFAULT_TIMEOUT, McuMgrImageResponse.class, callback);
    }

    /**
     * Erase a core dump from the device (synchronous).
     *
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrImageResponse coreErase() throws McuMgrException {
        return send(OP_WRITE, ID_CORELOAD, null, DEFAULT_TIMEOUT, McuMgrImageResponse.class);
    }

    //******************************************************************
    // Core Download
    //******************************************************************

    /**
     * Start core download.
     * <p>
     * Multiple calls will queue multiple downloads, executed sequentially. This includes image
     * uploads executed from {@link #imageUpload}.
     * <p>
     * The download may be controlled using the {@link TransferController} returned by this method.
     *
     * @param callback Receives callbacks from the download.
     * @return The object used to control this download.
     * @see TransferController
     * @see CoreDump
     */
    @NotNull
    public TransferController coreDownload(@NotNull DownloadCallback callback) {
        return startDownload(new CoreDownload(callback));
    }

    /**
     * Core Download Implementation
     */
    public class CoreDownload extends Download {
        protected CoreDownload(@NotNull DownloadCallback callback) {
            super(callback);
        }

        @Override
        public DownloadResponse read(int offset) throws McuMgrException {
            return coreLoad(offset);
        }
    }

    //******************************************************************
    // Image Upload
    //******************************************************************

    /**
     * Start image upload.
     * <p>
     * Multiple calls will queue multiple uploads, executed sequentially. This includes core
     * downloads executed from {@link #coreDownload}.
     * <p>
     * The upload may be controlled using the {@link TransferController} returned by this method.
     *
     * @param imageData The image data to upload.
     * @param callback  Receives callbacks from the upload.
     * @return The object used to control this upload.
     * @see TransferController
     */
    @NotNull
    public TransferController imageUpload(byte @NotNull [] imageData,
                                          @NotNull UploadCallback callback) {
        return imageUpload(imageData, 0, callback);
    }

    /**
     * Start image upload.
     * <p>
     * Multiple calls will queue multiple uploads, executed sequentially. This includes core
     * downloads executed from {@link #coreDownload}.
     * <p>
     * The upload may be controlled using the {@link TransferController} returned by this method.
     *
     * @param imageData The image data to upload.
     * @param image     The image number, default is 0. Use 0 for core0, 1 for core1, etc.
     * @param callback  Receives callbacks from the upload.
     * @return The object used to control this upload.
     * @see TransferController
     */
    @NotNull
    public TransferController imageUpload(byte @NotNull [] imageData, int image,
                                          @NotNull UploadCallback callback) {
        return startUpload(new ImageUpload(imageData, image, callback));
    }

    /**
     * Image Upload Implementation
     */
    public class ImageUpload extends Upload {
        private final int mImage;

        protected ImageUpload(byte @NotNull [] imageData, int image, @NotNull UploadCallback callback) {
            super(imageData, callback);
            mImage = image;
        }

        @Override
        protected UploadResponse write(byte @NotNull [] data, int offset) throws McuMgrException {
            return upload(data, offset, mImage);
        }
    }

    //******************************************************************
    // Image Upload (OLD, DEPRECATED)
    //******************************************************************

    // Upload states
    public final static int STATE_NONE = 0;
    public final static int STATE_UPLOADING = 1;
    public final static int STATE_PAUSED = 2;

    // Upload variables
    private int mUploadState = STATE_NONE;
    private int mUploadOffset = 0;
    private byte[] mImageData;
    @SuppressWarnings("deprecation")
    private ImageUploadCallback mUploadCallback;


    /**
     * Begin an image upload.
     * <p>
     * Only one upload can occur per ImageManager.
     *
     * @param data     the image data to upload to slot 1.
     * @param callback the image upload callback.
     * @return True, if the upload has stared, false otherwise.
     * @deprecated Use the new transfer implementation's imageUpload(...) method
     */
    @SuppressWarnings("deprecation")
    @Deprecated
    public synchronized boolean upload(byte @NotNull [] data, @NotNull ImageUploadCallback callback) {
        if (mUploadState == STATE_NONE) {
            mUploadState = STATE_UPLOADING;
        } else {
            LOG.debug("An image upload is already in progress");
            return false;
        }

        mUploadCallback = callback;
        mImageData = data;

        sendNext(0);
        return true;
    }

    /**
     * Get the current upload state ({@link ImageManager#STATE_NONE},
     * {@link ImageManager#STATE_UPLOADING}, {@link ImageManager#STATE_PAUSED}).
     *
     * @return The current upload state.
     * @deprecated Use the new transfer implementation's imageUpload(...) method
     */
    @Deprecated
    public synchronized int getUploadState() {
        return mUploadState;
    }

    /**
     * Cancel an image upload. Does nothing if an image upload is not in progress.
     * @deprecated Use the new transfer implementation's imageUpload(...) method
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    public synchronized void cancelUpload() {
        if (mUploadState == STATE_NONE) {
            LOG.debug("Image upload is not in progress");
        } else if (mUploadState == STATE_PAUSED) {
            LOG.info("Upload canceled");
            resetUpload();
            mUploadCallback.onUploadCanceled();
            mUploadCallback = null;
        }
        mUploadState = STATE_NONE;
    }

    /**
     * Pause an in progress upload.
     * @deprecated Use the new transfer implementation's imageUpload(...) method
     */
    @Deprecated
    public synchronized void pauseUpload() {
        if (mUploadState == STATE_NONE) {
            LOG.debug("Upload is not in progress.");
        } else {
            LOG.info("Upload paused");
            mUploadState = STATE_PAUSED;
        }
    }

    /**
     * Continue a paused image upload.
     * @deprecated Use the new transfer implementation's imageUpload(...) method
     */
    @Deprecated
    public synchronized void continueUpload() {
        if (mUploadState == STATE_PAUSED) {
            LOG.info("Continuing upload...");
            mUploadState = STATE_UPLOADING;
            sendNext(mUploadOffset);
        } else {
            LOG.debug("Upload is not paused.");
        }
    }

    private synchronized void failUpload(McuMgrException error) {
        if (mUploadCallback != null) {
            mUploadCallback.onUploadFailed(error);
        }
        //noinspection deprecation
        cancelUpload();
    }

    @SuppressWarnings("deprecation")
    private synchronized void restartUpload() {
        if (mImageData == null || mUploadCallback == null) {
            LOG.error("Could not restart upload: image data or callback is null!");
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
            LOG.debug("Image Manager is not in the UPLOADING state.");
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
            new McuMgrCallback<>() {
                @Override
                public void onResponse(@NotNull McuMgrImageUploadResponse response) {
                    // Check for a McuManager error.
                    if (response.rc != 0) {
                        // TODO when the image in slot 1 is confirmed, this will return ENOMEM (2).
                        LOG.error("Upload failed due to McuManager error: {}", response.rc);
                        failUpload(new McuMgrErrorException(McuMgrErrorCode.valueOf(response.rc)));
                        return;
                    }

                    // Get the next offset to send image data from.
                    mUploadOffset = response.off;

                    // Call the progress callback.
                    mUploadCallback.onProgressChanged(mUploadOffset, mImageData.length,
                            System.currentTimeMillis());

                    if (mUploadState == STATE_NONE) {
                        LOG.debug("Upload canceled!");
                        resetUpload();
                        mUploadCallback.onUploadCanceled();
                        mUploadCallback = null;
                        return;
                    }

                    // Check if the upload has finished.
                    if (mUploadOffset == mImageData.length) {
                        LOG.debug("Upload finished!");
                        resetUpload();
                        mUploadCallback.onUploadFinished();
                        mUploadCallback = null;
                        return;
                    }

                    // Send the next packet of upload data from the offset provided in the response.
                    sendNext(mUploadOffset);
                }

                @Override
                public void onError(@NotNull McuMgrException error) {
                    // Check if the exception is due to an insufficient MTU.
                    if (error instanceof InsufficientMtuException) {
                        final InsufficientMtuException mtuErr = (InsufficientMtuException) error;

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

    private int calculatePacketOverhead(byte @NotNull [] data, int offset, int image) {
        try {
            if (getScheme().isCoap()) {
                HashMap<String, Object> overheadTestMap = new HashMap<>();
                overheadTestMap.put("data", new byte[0]);
                overheadTestMap.put("off", offset);
                if (offset == 0) {
                    if (image > 0) {
                        overheadTestMap.put("image", image);
                    }
                    overheadTestMap.put("len", data.length);
                    overheadTestMap.put("sha", new byte[IMG_HASH_LEN]);
                }
                byte[] header = {0, 0, 0, 0, 0, 0, 0, 0};
                overheadTestMap.put("_h", header);
                byte[] cborData = CBOR.toBytes(overheadTestMap);
                // 20 byte estimate of CoAP Header; 5 bytes for good measure
                return cborData.length + 20 + 5;
            } else {
                // The code below removes the need of calling an expensive method CBOR.toBytes(..)
                // by calculating the overhead manually. Mind, that the data itself are not added.
                int size = 1;  // map: 0xAn (or 0xBn) - Map in a canonical form (max 23 pairs)
                size += 5 + CBOR.uintLength(data.length); // "data": 0x6464617461 + 3 for encoding length (as 16-bin positive int, worse case scenario) + NO DATA
                size += 4 + CBOR.uintLength(offset); // "off": 0x636F6666 + 3 bytes for the offset (as 16-bin positive int, worse case scenario)
                if (offset == 0) {
                    if (image > 0) {
                        size += 6 + 1; // ""image": 0x65696D616765 + 1 byte positive int
                    }
                    size += 4 + 5; // "len": 0x636C656E + len as 32-bit positive integer
                    size += 4 + 2 + 32; // "sha": 0x63736861 + 0x5820 + 32 bytes
                }
                return size + 8; // 8 additional bytes for the SMP header
            }
        } catch (IOException e) {
            LOG.error("Error while calculating packet overhead", e);
        }
        return -1;
    }

    /**
     * Callback for upload command.
     * @deprecated Use the new transfer implementation's UploadCallback
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    public interface ImageUploadCallback {

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
        void onUploadFailed(@NotNull McuMgrException error);

        /**
         * Called when the upload has been canceled.
         */
        void onUploadCanceled();

        /**
         * Called when the upload has finished successfully.
         */
        void onUploadFinished();
    }
}
