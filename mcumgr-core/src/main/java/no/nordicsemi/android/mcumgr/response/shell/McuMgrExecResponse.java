/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.response.shell;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nordicsemi.android.mcumgr.managers.ShellManager;
import no.nordicsemi.android.mcumgr.response.McuMgrResponse;

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
