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
import io.runtime.mcumgr.response.img.McuMgrImageUploadResponse;
import io.runtime.mcumgr.response.suit.McuMgrEnvelopeUploadResponse;
import io.runtime.mcumgr.response.suit.McuMgrManifestListResponse;
import io.runtime.mcumgr.response.suit.McuMgrManifestStateResponse;
import io.runtime.mcumgr.util.CBOR;

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
    private final static int ID_UPLOAD = 2;

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
     *
     * @param data     image data.
     * @param offset   the offset, from which the chunk will be sent.
     * @param callback the asynchronous callback.
     */
    public void upload(byte @NotNull [] data, int offset,
                       @NotNull McuMgrCallback<McuMgrEnvelopeUploadResponse> callback) {
        HashMap<String, Object> payloadMap = buildUploadPayload(data, offset);
        // Timeout for the initial chunk is long, as the device may need to erase the flash.
        final long timeout = offset == 0 ? DEFAULT_TIMEOUT : SHORT_TIMEOUT;
        send(OP_WRITE, ID_UPLOAD, payloadMap, timeout, McuMgrEnvelopeUploadResponse.class, callback);
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
     *
     * @param data   image data.
     * @param offset the offset, from which the chunk will be sent.
     * @return The upload response.
     */
    @NotNull
    public McuMgrImageUploadResponse upload(byte @NotNull [] data, int offset)
            throws McuMgrException {
        HashMap<String, Object> payloadMap = buildUploadPayload(data, offset);
        // Timeout for the initial chunk is long, as the device may need to erase the flash.
        final long timeout = offset == 0 ? DEFAULT_TIMEOUT : SHORT_TIMEOUT;
        return send(OP_WRITE, ID_UPLOAD, payloadMap, timeout, McuMgrImageUploadResponse.class);
    }

    /*
     * Build the upload payload.
     */
    @NotNull
    private HashMap<String, Object> buildUploadPayload(byte @NotNull [] data, int offset) {
        // Get chunk of image data to send
        int dataLength = Math.min(mMtu - calculatePacketOverhead(data, offset), data.length - offset);
        byte[] sendBuffer = new byte[dataLength];
        System.arraycopy(data, offset, sendBuffer, 0, dataLength);

        // Create the map of key-values for the McuManager payload
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("data", sendBuffer);
        payloadMap.put("off", offset);
        if (offset == 0) {
            payloadMap.put("len", data.length);
        }
        return payloadMap;
    }

    private int calculatePacketOverhead(byte @NotNull [] data, int offset) {
        try {
            if (getScheme().isCoap()) {
                HashMap<String, Object> overheadTestMap = new HashMap<>();
                overheadTestMap.put("data", new byte[0]);
                overheadTestMap.put("off", offset);
                if (offset == 0) {
                    overheadTestMap.put("len", data.length);
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
                    size += 4 + 5; // "len": 0x636C656E + len as 32-bit positive integer
                }
                return size + 8; // 8 additional bytes for the SMP header
            }
        } catch (IOException e) {
            LOG.error("Error while calculating packet overhead", e);
        }
        return -1;
    }


}
