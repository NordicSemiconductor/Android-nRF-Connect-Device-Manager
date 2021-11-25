/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.viewmodel.mcumgr;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import javax.inject.Inject;
import javax.inject.Named;

import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.sample.observable.BondingState;
import io.runtime.mcumgr.sample.observable.ConnectionState;
import io.runtime.mcumgr.sample.observable.ObservableMcuMgrBleTransport;

public class DeviceStatusViewModel extends McuMgrViewModel {
    private final LiveData<ConnectionState> connectionStateLiveData;
    private final LiveData<BondingState> bondStateLiveData;

    @Inject
    DeviceStatusViewModel(final McuMgrTransport transport,
                          @Named("busy") final MutableLiveData<Boolean> state) {
        super(state);

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
    }

    public LiveData<ConnectionState> getConnectionState() {
        return connectionStateLiveData;
    }

    public LiveData<BondingState> getBondState() {
        return bondStateLiveData;
    }

}
