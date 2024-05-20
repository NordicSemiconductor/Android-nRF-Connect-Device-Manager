package io.runtime.mcumgr.managers;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;

import io.runtime.mcumgr.McuManager;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.InsufficientMtuException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.response.suit.McuMgrUploadResponse;
import io.runtime.mcumgr.response.suit.McuMgrManifestListResponse;
import io.runtime.mcumgr.response.suit.McuMgrManifestStateResponse;
import io.runtime.mcumgr.response.suit.McuMgrPollResponse;
import io.runtime.mcumgr.transfer.EnvelopeUploader;
import io.runtime.mcumgr.transfer.ResourceUploader;
import io.runtime.mcumgr.transfer.UploadCallback;
import io.runtime.mcumgr.util.CBOR;
import kotlinx.coroutines.CoroutineScope;

/**
 * The SUIT Manager provides API to access SUIT manifests on supported devices, as well as
 * perform firmware update. Comparing to {@link ImageManager} it provides more granular control
 * over the running firmware split into several domains.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class SUITManager extends McuManager {
    private final static Logger LOG = LoggerFactory.getLogger(SUITManager.class);

    /**
     * Command allows to get information about roles of manifests supported by the device.
     */
    private final static int ID_MANIFEST_LIST = 0;

    /**
     * Command allows to get information about the configuration of supported manifests
     * and selected attributes of installed manifests of specified role.
     */
    private final static int ID_MANIFEST_STATE = 1;

    /**
     * Command delivers a packet of a SUIT envelope to the device.
     */
    private final static int ID_ENVELOPE_UPLOAD = 2;

    /**
     * SUIT command sequence has the ability of conditional execution of directives, i.e. based
     * on the digest of installed image. That opens scenario where SUIT candidate envelope contains
     * only SUIT manifests, images (those required to be updated) are fetched by the device only
     * if it is necessary. In that case, the device informs the SMP client that specific image
     * is required (and this is what this command implements), and then the SMP client delivers
     * requested image in chunks. Due to the fact that SMP is designed in clients-server pattern
     * and lack of server-sent notifications, implementation bases on polling.
     */
    private final static int ID_POLL_IMAGE_STATE = 3;

    /**
     * Command delivers a packet of a resource requested by the target device.
     */
    private final static int ID_RESOURCE_UPLOAD = 4;

    /**
     * Construct a McuManager instance.
     *
     * @param transporter the transporter to use to send commands.
     */
    public SUITManager(@NotNull McuMgrTransport transporter) {
        super(McuManager.GROUP_SUIT, transporter);
    }

    /**
     * Command allows to get information about roles of manifests supported by the device.
     * @return The response with the list of manifest roles. Use {@link #getManifestState(int)}
     * to get more information about the manifest.
     * @throws McuMgrException on failure.
     */
    @NotNull
    public McuMgrResponse listManifests() throws McuMgrException {
        return send(OP_READ, ID_MANIFEST_LIST, null, SHORT_TIMEOUT, McuMgrManifestListResponse.class);
    }

    /**
     * Command allows to get information about roles of manifests supported by the device.
     * <p>
     * The response contains the list of manifest roles. Use {@link #getManifestState(int, McuMgrCallback)}
     * to get more information about the manifest.
     * @param callback The response callback.
     */
    public void listManifests(@NotNull McuMgrCallback<McuMgrManifestListResponse> callback) {
        send(OP_READ, ID_MANIFEST_LIST, null, SHORT_TIMEOUT, McuMgrManifestListResponse.class, callback);
    }

    /**
     * Command allows to get information about the configuration of supported manifests
     * and selected attributes of installed manifests of specified role (asynchronous).
     *
     * @param role Manifest role, one of values returned by {@link #listManifests()} command.
     * @param callback the asynchronous callback.
     */
    public void getManifestState(int role, @NotNull McuMgrCallback<McuMgrManifestStateResponse> callback) {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("role", role);
        send(OP_READ, ID_MANIFEST_STATE, payloadMap, SHORT_TIMEOUT, McuMgrManifestStateResponse.class, callback);
    }

    /**
     * Command allows to get information about the configuration of supported manifests
     * and selected attributes of installed manifests of specified role (synchronous).
     *
     * @param role Manifest role, one of values returned by {@link #listManifests()} command.
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrManifestStateResponse getManifestState(int role) throws McuMgrException {
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("role", role);
        return send(OP_READ, ID_MANIFEST_STATE, payloadMap, SHORT_TIMEOUT, McuMgrManifestStateResponse.class);
    }

    /**
     * Command delivers a part of SUIT envelope to the device (asynchronous).
     * <p>
     * Once upload is completed the device validates delivered envelope and starts SUIT processing.
     * <p>
     * The chunk size is limited by the current MTU. If the current MTU set by
     * {@link #setUploadMtu(int)} is too large, the {@link McuMgrCallback#onError(McuMgrException)}
     * with {@link InsufficientMtuException} error will be returned.
     * Use {@link InsufficientMtuException#getMtu()} to get the current MTU and
     * pass it to {@link #setUploadMtu(int)} and try again.
     * <p>
     * Use {@link EnvelopeUploader#uploadAsync(UploadCallback)} to
     * upload the whole envelope.
     *
     * @param data     image data.
     * @param offset   the offset, from which the chunk will be sent.
     * @param callback the asynchronous callback.
     */
    public void upload(byte @NotNull [] data, int offset,
                       @NotNull McuMgrCallback<McuMgrUploadResponse> callback) {
        HashMap<String, Object> payloadMap = buildUploadPayload(data, offset);
        // Timeout for the initial chunk is long, as the device may need to erase the flash.
        final long timeout = offset == 0 ? DEFAULT_TIMEOUT : SHORT_TIMEOUT;
        send(OP_WRITE, ID_ENVELOPE_UPLOAD, payloadMap, timeout, McuMgrUploadResponse.class, callback);
    }

    /**
     * Command delivers a part of SUIT envelope to the device (synchronous).
     * <p>
     * Once upload is completed the device validates delivered envelope and starts SUIT processing.
     * <p>
     * The chunk size is limited by the current MTU. If the current MTU set by
     * {@link #setUploadMtu(int)} is too large, the {@link InsufficientMtuException} error will be
     * thrown. Use {@link InsufficientMtuException#getMtu()} to get the current MTU and
     * pass it to {@link #setUploadMtu(int)} and try again.
     * <p>
     * Use {@link EnvelopeUploader#uploadAsync(UploadCallback, CoroutineScope)}
     * to upload the whole envelope.
     *
     * @param data   image data.
     * @param offset the offset, from which the chunk will be sent.
     * @return The upload response.
     */
    @NotNull
    public McuMgrUploadResponse upload(byte @NotNull [] data, int offset)
            throws McuMgrException {
        HashMap<String, Object> payloadMap = buildUploadPayload(data, offset);
        // Timeout for the initial chunk is long, as the device may need to erase the flash.
        final long timeout = offset == 0 ? DEFAULT_TIMEOUT : SHORT_TIMEOUT;
        return send(OP_WRITE, ID_ENVELOPE_UPLOAD, payloadMap, timeout, McuMgrUploadResponse.class);
    }

    /**
     * Poll for required image (asynchronous).
     * <p>
     * SUIT command sequence has the ability of conditional execution of directives, i.e. based
     * on the digest of installed image. That opens scenario where SUIT candidate envelope contains
     * only SUIT manifests, images (those required to be updated) are fetched by the device only
     * if it is necessary. In that case, the device informs the SMP client that specific image
     * is required (and this is what this command implements), and then the SMP client delivers
     * requested image in chunks. Due to the fact that SMP is designed in clients-server pattern
     * and lack of server-sent notifications, implementation bases on polling.
     * <p>
     * After sending the Envelope, the client should periodically poll the device to check if an
     * image is required.
     *
     * @param callback the asynchronous callback.
     */
    public void poll(@NotNull McuMgrCallback<McuMgrPollResponse> callback) {
        send(OP_READ, ID_POLL_IMAGE_STATE, null, SHORT_TIMEOUT, McuMgrPollResponse.class, callback);
    }

    /**
     * Poll for required image (synchronous).
     * <p>
     * SUIT command sequence has the ability of conditional execution of directives, i.e. based
     * on the digest of installed image. That opens scenario where SUIT candidate envelope contains
     * only SUIT manifests, images (those required to be updated) are fetched by the device only
     * if it is necessary. In that case, the device informs the SMP client that specific image
     * is required (and this is what this command implements), and then the SMP client delivers
     * requested image in chunks. Due to the fact that SMP is designed in clients-server pattern
     * and lack of server-sent notifications, implementation bases on polling.
     * <p>
     * After sending the Envelope, the client should periodically poll the device to check if an
     * image is required.
     *
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public McuMgrPollResponse poll() throws McuMgrException {
        return send(OP_READ, ID_POLL_IMAGE_STATE, null, SHORT_TIMEOUT, McuMgrPollResponse.class);
    }

    /**
     * Command delivers a part of the requested resource to the device (asynchronous).
     * <p>
     * After sending a SUIT Envelope using {@link #upload(byte[], int, McuMgrCallback)}, the device
     * may request a resource to be delivered. To get the session ID and the resource ID, use
     * {@link #poll(McuMgrCallback)}.
     * <p>
     * Use {@link ResourceUploader#uploadAsync(UploadCallback)} to
     * upload the whole file.
     *
     * @param sessionId the non-zero session ID obtained using {@link #poll()}.
     * @param data     image data.
     * @param offset   the offset, from which the chunk will be sent.
     * @param callback the asynchronous callback.
     */
    public void uploadResource(int sessionId, byte @NotNull [] data, int offset,
                       @NotNull McuMgrCallback<McuMgrUploadResponse> callback) {
        if (sessionId <= 0) {
            throw new IllegalArgumentException("Session ID must be greater than 0");
        }
        HashMap<String, Object> payloadMap = buildUploadPayload(data, offset, sessionId);
        // Timeout for the initial chunk is long, as the device may need to erase the flash.
        final long timeout = offset == 0 ? DEFAULT_TIMEOUT : SHORT_TIMEOUT;
        send(OP_WRITE, ID_RESOURCE_UPLOAD, payloadMap, timeout, McuMgrUploadResponse.class, callback);
    }

    /**
     * Command delivers a part of the requested resource to the device (synchronous).
     * <p>
     * After sending a SUIT Envelope using {@link #upload(byte[], int)}, the device
     * may request a resource to be delivered. To get the session ID and the resource ID, use
     * {@link #poll()}.
     * <p>
     * Use {@link ResourceUploader#uploadAsync(UploadCallback, CoroutineScope)}
     * to upload the whole envelope.
     *
     * @param sessionId the non-zero session ID obtained using {@link #poll()}.
     * @param data   image data.
     * @param offset the offset, from which the chunk will be sent.
     * @return The upload response.
     */
    @NotNull
    public McuMgrUploadResponse uploadResource(int sessionId, byte @NotNull [] data, int offset)
            throws McuMgrException {
        if (sessionId <= 0) {
            throw new IllegalArgumentException("Session ID must be greater than 0");
        }
        HashMap<String, Object> payloadMap = buildUploadPayload(data, offset, sessionId);
        // Timeout for the initial chunk is long, as the device may need to erase the flash.
        final long timeout = offset == 0 ? DEFAULT_TIMEOUT : SHORT_TIMEOUT;
        return send(OP_WRITE, ID_RESOURCE_UPLOAD, payloadMap, timeout, McuMgrUploadResponse.class);
    }

    /*
     * Build the upload payload.
     */
    @NotNull
    private HashMap<String, Object> buildUploadPayload(byte @NotNull [] data, int offset) {
        return buildUploadPayload(data, offset, -1);
    }

    @NotNull
    private HashMap<String, Object> buildUploadPayload(byte @NotNull [] data, int offset, int sessionId) {
        // Get chunk of image data to send
        int dataLength = Math.min(mMtu - calculatePacketOverhead(data, offset, sessionId), data.length - offset);
        byte[] sendBuffer = new byte[dataLength];
        System.arraycopy(data, offset, sendBuffer, 0, dataLength);

        // Create the map of key-values for the McuManager payload
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("data", sendBuffer);
        payloadMap.put("off", offset);
        if (offset == 0) {
            payloadMap.put("len", data.length);
            if (sessionId > 0) {
                payloadMap.put("stream_session_id", sessionId);
            }
        }
        return payloadMap;
    }

    private int calculatePacketOverhead(byte @NotNull [] data, int offset, int sessionId) {
        try {
            if (getScheme().isCoap()) {
                HashMap<String, Object> overheadTestMap = new HashMap<>();
                overheadTestMap.put("data", new byte[0]);
                overheadTestMap.put("off", offset);
                if (offset == 0) {
                    overheadTestMap.put("len", data.length);
                    if (sessionId > 0) {
                        overheadTestMap.put("stream_session_id", sessionId);
                    }
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
                size += 5 + CBOR.uintLength(data.length); // "data": 0x6464617461 + length + NO DATA
                size += 4 + CBOR.uintLength(offset); // "off": 0x636F6666 + offset
                if (offset == 0) {
                    size += 4 + 5; // "len": 0x636C656E + len as 32-bit positive integer
                    if (sessionId > 0) {
                        size += 18 + CBOR.uintLength(sessionId); // "stream_session_id": 0x73747265616D5F73657373696F6E5F6964 + session ID
                    }
                }
                return size + 8; // 8 additional bytes for the SMP header
            }
        } catch (IOException e) {
            LOG.error("Error while calculating packet overhead", e);
        }
        return -1;
    }


}
