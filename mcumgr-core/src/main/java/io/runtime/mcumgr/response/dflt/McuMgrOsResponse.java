package io.runtime.mcumgr.response.dflt;

import com.fasterxml.jackson.annotation.JsonCreator;

import io.runtime.mcumgr.managers.DefaultManager;
import io.runtime.mcumgr.response.McuMgrResponse;

public class McuMgrOsResponse extends McuMgrResponse implements DefaultManager.Response {

    @JsonCreator
    public McuMgrOsResponse() {}
}
