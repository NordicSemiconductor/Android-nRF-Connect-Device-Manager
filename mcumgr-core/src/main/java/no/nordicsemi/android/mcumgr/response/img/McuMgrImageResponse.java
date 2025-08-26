package no.nordicsemi.android.mcumgr.response.img;

import com.fasterxml.jackson.annotation.JsonCreator;

import no.nordicsemi.android.mcumgr.managers.DefaultManager;
import no.nordicsemi.android.mcumgr.response.McuMgrResponse;

public class McuMgrImageResponse extends McuMgrResponse implements DefaultManager.Response {

    @JsonCreator
    public McuMgrImageResponse() {}
}
