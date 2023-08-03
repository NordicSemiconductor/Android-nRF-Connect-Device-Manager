/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.viewmodel.mcumgr;

import javax.inject.Inject;
import javax.inject.Named;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.managers.ShellManager;
import io.runtime.mcumgr.response.shell.McuMgrExecResponse;
import io.runtime.mcumgr.sample.viewmodel.SingleLiveEvent;

public class ExecViewModel extends McuMgrViewModel {
    private final ShellManager manager;

    private final MutableLiveData<String> commandLiveData = new SingleLiveEvent<>();
    private final MutableLiveData<String> outputLiveData = new SingleLiveEvent<>();
    private final MutableLiveData<McuMgrException> errorLiveData = new SingleLiveEvent<>();

    @Inject
    ExecViewModel(final ShellManager manager,
                  @Named("busy") final MutableLiveData<Boolean> state) {
        super(state);
        this.manager = manager;
    }

    @NonNull
    public LiveData<String> getCommand() {
        return commandLiveData;
    }

    @NonNull
    public LiveData<String> getOutput() {
        return outputLiveData;
    }

    @NonNull
    public LiveData<McuMgrException> getError() {
        return errorLiveData;
    }

    public void exec(@NonNull final String command, @Nullable final String[] argv) {
        setBusy();
        commandLiveData.postValue(command);
        manager.exec(command, argv, new McuMgrCallback<>() {
            @Override
            public void onResponse(@NonNull final McuMgrExecResponse response) {
                outputLiveData.postValue(response.o);
                postReady();
            }

            @Override
            public void onError(@NonNull final McuMgrException error) {
                errorLiveData.postValue(error);
                postReady();
            }
        });
    }
}
