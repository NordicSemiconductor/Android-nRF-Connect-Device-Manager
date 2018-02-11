package io.runtime.mcumgr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

public interface McuMgrResponse {

    McuMgrHeader getHeader();
    int getRcValue();
    McuManager.Code getRc();
    Map<String, Object> getPayloadMap();
    byte[] getBytes();
    byte[] getPayload();
    boolean isSuccess();
    McuManager.Scheme getScheme();

    @JsonIgnoreProperties(ignoreUnknown=true)
    class BaseResponse {
        public int rc;
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    class CoapBaseResponse {
        public byte[] _h;
        public int rc;
    }
}
