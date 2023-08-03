package io.runtime.mcumgr.response.zephyr.basic;

import com.fasterxml.jackson.annotation.JsonCreator;

import io.runtime.mcumgr.managers.BasicManager;
import io.runtime.mcumgr.response.McuMgrResponse;

public class McuMgrZephyrBasicResponse extends McuMgrResponse implements BasicManager.Response {

    @JsonCreator
    public McuMgrZephyrBasicResponse() {}
}
