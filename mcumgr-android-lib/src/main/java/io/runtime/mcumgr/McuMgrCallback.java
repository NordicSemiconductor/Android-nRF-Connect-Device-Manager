package io.runtime.mcumgr;

import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.resp.McuMgrResponse;

/**
 * Callback for asynchronous Newt Manager commands.
 */
public interface McuMgrCallback<T extends McuMgrResponse> {
    /**
     * Newt Manager has received a response.
     * @param response the response
     */
    void onResponse(T response);

    /**
     * Newt Manager has encountered a transport error while sending the command.
     * @param error the error
     */
    void onError(McuMgrException error);
}
