package no.nordicsemi.android.mcumgr.response.dflt;

import com.fasterxml.jackson.annotation.JsonCreator;

import no.nordicsemi.android.mcumgr.managers.DefaultManager;
import no.nordicsemi.android.mcumgr.response.McuMgrResponse;

public class McuMgrOsResponse extends McuMgrResponse implements DefaultManager.Response {

    @JsonCreator
    public McuMgrOsResponse() {}
}
