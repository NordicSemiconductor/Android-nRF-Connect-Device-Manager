package io.runtime.mcumgr;

import io.runtime.mcumgr.exception.McuMgrException;

/**
 * Callback for asynchronous Newt Manager commands.
 */
public interface McuMgrCallback {
    /**
     * Newt Manager has received a response.
     * @param response the response
     */
    void onResponse(McuMgrResponse response);

    /**
     * Newt Manager has encountered a transport error while sending the command.
     * @param error the error
     */
    void onError(McuMgrException error);
}
