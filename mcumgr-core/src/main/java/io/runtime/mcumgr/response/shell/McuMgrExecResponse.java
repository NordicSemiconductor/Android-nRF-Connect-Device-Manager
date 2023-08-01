/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.shell;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.runtime.mcumgr.managers.ShellManager;
import io.runtime.mcumgr.response.McuMgrResponse;

public class McuMgrExecResponse extends McuMgrResponse implements ShellManager.Response {
    /** The command output. */
    @JsonProperty("o")
    public String o;

    /** Return code from shell command execution. */
    @JsonProperty("ret")
    public int ret;

    @JsonCreator
    public McuMgrExecResponse() {}
}
