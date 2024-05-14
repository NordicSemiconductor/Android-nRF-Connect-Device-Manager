package io.runtime.mcumgr.managers;

import org.jetbrains.annotations.NotNull;

import io.runtime.mcumgr.McuManager;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.response.suit.McuMgrManifestListResponse;

@SuppressWarnings({"unused", "WeakerAccess"})
public class SUITManager extends McuManager {

    /**
     * Command allows to get information about roles of manifests supported by the device.
     */
    private final static int ID_MANIFEST_LIST = 0;

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
     * @return The response
     * @throws McuMgrException on failure.
     */
    @NotNull
    public McuMgrResponse listManifests() throws McuMgrException {
        return send(OP_READ, ID_MANIFEST_LIST, null, SHORT_TIMEOUT, McuMgrManifestListResponse.class);
    }

    /**
     * Command allows to get information about roles of manifests supported by the device.
     * @param callback The response callback.
     */
    public void listManifests(@NotNull McuMgrCallback<McuMgrManifestListResponse> callback) {
        send(OP_READ, ID_MANIFEST_LIST, null, SHORT_TIMEOUT, McuMgrManifestListResponse.class, callback);
    }
}
