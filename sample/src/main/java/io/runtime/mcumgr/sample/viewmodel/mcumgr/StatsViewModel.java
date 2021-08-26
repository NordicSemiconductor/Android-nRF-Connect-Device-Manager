/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.viewmodel.mcumgr;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.managers.StatsManager;
import io.runtime.mcumgr.response.stat.McuMgrStatListResponse;
import io.runtime.mcumgr.response.stat.McuMgrStatResponse;

public class StatsViewModel extends McuMgrViewModel {
    private final StatsManager mManager;

    private final MutableLiveData<List<McuMgrStatResponse>> mResponseLiveData = new MutableLiveData<>();
    private final MutableLiveData<McuMgrException> mErrorLiveData = new MutableLiveData<>();

    @Inject
    StatsViewModel(final StatsManager manager,
                   @Named("busy") final MutableLiveData<Boolean> state) {
        super(state);
        mManager = manager;
    }

    public LiveData<List<McuMgrStatResponse>> getResponse() {
        return mResponseLiveData;
    }

    @NonNull
    public LiveData<McuMgrException> getError() {
        return mErrorLiveData;
    }

    public void readStats() {
        setBusy();
        mErrorLiveData.setValue(null);
        mManager.list(new McuMgrCallback<McuMgrStatListResponse>() {
            @Override
            public void onResponse(@NonNull final McuMgrStatListResponse listResponse) {
                final List<McuMgrStatResponse> list = new ArrayList<>(listResponse.stat_list.length);

                // Request stats for each module
                for (final String module : listResponse.stat_list) {
                    mManager.read(module, new McuMgrCallback<McuMgrStatResponse>() {
                        @Override
                        public void onResponse(@NonNull final McuMgrStatResponse response) {
                            list.add(response);
                            mResponseLiveData.postValue(list);
                            postReady();
                        }

                        @Override
                        public void onError(@NonNull final McuMgrException error) {
                            mErrorLiveData.postValue(error);
                            postReady();
                        }
                    });
                }
            }

            @Override
            public void onError(@NonNull final McuMgrException error) {
                mErrorLiveData.postValue(error);
                postReady();
            }
        });
    }
}
