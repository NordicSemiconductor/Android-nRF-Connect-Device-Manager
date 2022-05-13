/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.viewmodel.mcumgr;

import org.jetbrains.annotations.NotNull;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import javax.inject.Inject;
import javax.inject.Named;

import androidx.lifecycle.Observer;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.ble.McuMgrBleTransport;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.managers.DefaultManager;
import io.runtime.mcumgr.response.dflt.McuMgrParamsResponse;
import io.runtime.mcumgr.sample.observable.BondingState;
import io.runtime.mcumgr.sample.observable.ConnectionState;
import io.runtime.mcumgr.sample.observable.ObservableMcuMgrBleTransport;

public class DeviceStatusViewModel extends McuMgrViewModel {
    private final LiveData<ConnectionState> connectionStateLiveData;
    private final LiveData<BondingState> bondStateLiveData;

    private final MutableLiveData<McuMgrBufferParams> bufferLiveData = new MutableLiveData<>();
    private final Observer<ConnectionState> connectionStateObserver = new Observer<ConnectionState>() {
        @Override
        public void onChanged(final ConnectionState connectionState) {
            if (connectionState == ConnectionState.READY) {
                defaultManager.params(new McuMgrCallback<McuMgrParamsResponse>() {
                    @Override
                    public void onResponse(@NotNull final McuMgrParamsResponse response) {
                        bufferLiveData.postValue(new McuMgrBufferParams(response));
                    }

                    @Override
                    public void onError(@NotNull final McuMgrException error) {
                        final McuMgrTransport transport = defaultManager.getTransporter();
                        if (transport instanceof McuMgrBleTransport) {
                            final McuMgrBleTransport bleTransport = (McuMgrBleTransport) transport;
                            final int maxPacketLength = bleTransport.getMaxPacketLength();
                            final McuMgrBufferParams mcuParams = new McuMgrBufferParams(maxPacketLength);
                            bufferLiveData.postValue(mcuParams);
                        } else {
                            bufferLiveData.postValue(null);
                        }
                    }
                });
            } else {
                bufferLiveData.postValue(null);
            }
        }
    };

    private final DefaultManager defaultManager;

    @Inject
    DeviceStatusViewModel(final DefaultManager manager,
                          @Named("busy") final MutableLiveData<Boolean> state) {
        super(state);
        defaultManager = manager;

        final McuMgrTransport transport = manager.getTransporter();
        if (transport instanceof ObservableMcuMgrBleTransport) {
            connectionStateLiveData = ((ObservableMcuMgrBleTransport) transport).getState();
            bondStateLiveData = ((ObservableMcuMgrBleTransport) transport).getBondingState();
        } else {
            final MutableLiveData<ConnectionState> liveData = new MutableLiveData<>();
            transport.addObserver(new McuMgrTransport.ConnectionObserver() {
                @Override
                public void onConnected() {
                    liveData.postValue(ConnectionState.READY);
                }

                @Override
                public void onDisconnected() {
                    liveData.postValue(ConnectionState.DISCONNECTED);
                }
            });
            connectionStateLiveData = liveData;
            bondStateLiveData = new MutableLiveData<>(BondingState.NOT_BONDED);
        }
        connectionStateLiveData.observeForever(connectionStateObserver);
    }

    @Override
    protected void onCleared() {
        connectionStateLiveData.removeObserver(connectionStateObserver);
        super.onCleared();
    }

    public LiveData<ConnectionState> getConnectionState() {
        return connectionStateLiveData;
    }

    public LiveData<BondingState> getBondState() {
        return bondStateLiveData;
    }

    public LiveData<McuMgrBufferParams> getBufferParams() { return bufferLiveData; }

    public static class McuMgrBufferParams {
        public final int size;
        public final int count;

        private McuMgrBufferParams(@NonNull final McuMgrParamsResponse response) {
            size = response.bufSize;
            count = response.bufCount;
        }

        private McuMgrBufferParams(final int maxPacketLength) {
            size = maxPacketLength;
            count = 1;
        }
    }
}
