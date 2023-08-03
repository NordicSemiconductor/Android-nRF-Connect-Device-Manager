package io.runtime.mcumgr.response.img;

import com.fasterxml.jackson.annotation.JsonCreator;

import io.runtime.mcumgr.managers.DefaultManager;
import io.runtime.mcumgr.response.McuMgrResponse;

public class McuMgrImageResponse extends McuMgrResponse implements DefaultManager.Response {

    @JsonCreator
    public McuMgrImageResponse() {}
}
