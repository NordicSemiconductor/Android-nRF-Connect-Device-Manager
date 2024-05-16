package io.runtime.mcumgr.managers;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

import io.runtime.mcumgr.McuManager;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.response.suit.McuMgrManifestListResponse;
import io.runtime.mcumgr.response.suit.McuMgrManifestStateResponse;

/**
 * The SUIT Manager provides API to access SUIT manifests on supported devices, as well as
 * perform firmware update. Comparing to {@link ImageManager} it provides more granular control
 * over the running firmware split into several domains.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class SUITManager extends McuManager {

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
}
