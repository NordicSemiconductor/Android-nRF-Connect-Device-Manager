package io.runtime.mcumgr.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UploadResponse extends McuMgrResponse {
    /** The offset. Number of bytes that were received. */
    @JsonProperty("off")
    public int off;

    @JsonCreator
    public UploadResponse() {}
}
