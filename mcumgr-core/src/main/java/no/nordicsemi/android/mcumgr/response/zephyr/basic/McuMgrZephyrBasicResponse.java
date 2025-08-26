package no.nordicsemi.android.mcumgr.response.zephyr.basic;

import com.fasterxml.jackson.annotation.JsonCreator;

import no.nordicsemi.android.mcumgr.managers.BasicManager;
import no.nordicsemi.android.mcumgr.response.McuMgrResponse;

public class McuMgrZephyrBasicResponse extends McuMgrResponse implements BasicManager.Response {

    @JsonCreator
    public McuMgrZephyrBasicResponse() {}
}
