/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.viewmodel.mcumgr;

import android.content.Context;

import javax.inject.Inject;
import javax.inject.Named;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.exception.McuMgrTimeoutException;
import io.runtime.mcumgr.managers.DefaultManager;
import io.runtime.mcumgr.response.dflt.McuMgrEchoResponse;
import io.runtime.mcumgr.sample.R;

public class EchoViewModel extends McuMgrViewModel {
    private final DefaultManager mManager;

    private final MutableLiveData<String> mRequestLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> mResponseLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> mErrorLiveData = new MutableLiveData<>();

    @Inject
    Context mContext;

    @Inject
    EchoViewModel(final DefaultManager manager,
                  @Named("busy") final MutableLiveData<Boolean> state) {
        super(state);
        mManager = manager;
    }

    @NonNull
    public LiveData<String> getRequest() {
        return mRequestLiveData;
    }

    @NonNull
    public LiveData<String> getResponse() {
        return mResponseLiveData;
    }

    @NonNull
    public LiveData<String> getError() {
        return mErrorLiveData;
    }

    public void echo(final String echo) {
        setBusy();
        mRequestLiveData.postValue(echo);
        mManager.echo(echo, new McuMgrCallback<McuMgrEchoResponse>() {
            @Override
            public void onResponse(@NonNull final McuMgrEchoResponse response) {
                mResponseLiveData.postValue(response.r);
                postReady();
            }

            @Override
            public void onError(@NonNull final McuMgrException error) {
                if (error instanceof McuMgrTimeoutException) {
                    mErrorLiveData.postValue(mContext.getString(R.string.status_connection_timeout));
                } else {
                    mErrorLiveData.postValue(error.getMessage());
                }
                postReady();
            }
        });
    }
}
