package io.runtime.mcumgr.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DownloadResponse extends McuMgrResponse {
    /** The offset of the {@link #data}. */
    @JsonProperty("off")
    public int off;
    /** The total length of the data. Only sent in the initial packet. */
    @JsonProperty("len")
    public int len;
    /** The data. */
    @JsonProperty("data")
    public byte[] data;

    @JsonCreator
    public DownloadResponse() {}
}
