/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.viewmodel.mcumgr;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.managers.StatsManager;
import io.runtime.mcumgr.response.stat.McuMgrStatListResponse;
import io.runtime.mcumgr.response.stat.McuMgrStatResponse;

public class StatsViewModel extends McuMgrViewModel {
    private final StatsManager manager;

    private final MutableLiveData<List<McuMgrStatResponse>> responseLiveData = new MutableLiveData<>();
    private final MutableLiveData<McuMgrException> errorLiveData = new MutableLiveData<>();

    @Inject
    StatsViewModel(final StatsManager manager,
                   @Named("busy") final MutableLiveData<Boolean> state) {
        super(state);
        this.manager = manager;
    }

    public LiveData<List<McuMgrStatResponse>> getResponse() {
        return responseLiveData;
    }

    @NonNull
    public LiveData<McuMgrException> getError() {
        return errorLiveData;
    }

    public void readStats() {
        setBusy();
        errorLiveData.setValue(null);
        manager.list(new McuMgrCallback<>() {
            @Override
            public void onResponse(@NonNull final McuMgrStatListResponse listResponse) {
                final List<McuMgrStatResponse> list = new ArrayList<>(listResponse.stat_list.length);

                // Request stats for each module.
                if (listResponse.stat_list.length > 0) {
                    final String module = listResponse.stat_list[0];
                    manager.read(module, new McuMgrCallback<>() {
                        private int i = 1;

                        @Override
                        public void onResponse(@NonNull final McuMgrStatResponse response) {
                            list.add(response);
                            responseLiveData.postValue(list);

                            if (i < listResponse.stat_list.length) {
                                final String module = listResponse.stat_list[i++];
                                manager.read(module, this);
                            } else {
                                postReady();
                            }
                        }
                        @Override
                        public void onError(@NonNull final McuMgrException error) {
                            errorLiveData.postValue(error);
                            postReady();
                        }
                    });
                } else {
                    responseLiveData.postValue(list);
                    postReady();
                }
            }

            @Override
            public void onError(@NonNull final McuMgrException error) {
                errorLiveData.postValue(error);
                postReady();
            }
        });
    }
}
