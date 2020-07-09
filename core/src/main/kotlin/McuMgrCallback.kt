package com.juul.mcumgr

import com.juul.mcumgr.message.Response

interface McuMgrCallback<T : Response> {
    fun onSuccess(response: T)
    fun onError(code: Response.Code)
    fun onFailure(throwable: Throwable)
}
