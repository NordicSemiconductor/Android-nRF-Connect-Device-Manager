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
    private LiveData<ConnectionState> mConnectionStateLiveData;
    private LiveData<BondingState> mBondStateLiveData;

    @Inject
    DeviceStatusViewModel(final McuMgrTransport transport,
                          @Named("busy") final MutableLiveData<Boolean> state) {
        super(state);

        if (transport instanceof ObservableMcuMgrBleTransport) {
            mConnectionStateLiveData = ((ObservableMcuMgrBleTransport) transport).getState();
            mBondStateLiveData = ((ObservableMcuMgrBleTransport) transport).getBondingState();
        } else {
            final MutableLiveData<ConnectionState> connectionStateLiveData = new MutableLiveData<>();
            transport.addObserver(new McuMgrTransport.ConnectionObserver() {
                @Override
                public void onConnected() {
                    connectionStateLiveData.postValue(ConnectionState.READY);
                }

                @Override
                public void onDisconnected() {
                    connectionStateLiveData.postValue(ConnectionState.DISCONNECTED);
                }
            });
            mConnectionStateLiveData = connectionStateLiveData;
            mBondStateLiveData = new MutableLiveData<>(BondingState.NOT_BONDED);
        }
    }

    public LiveData<ConnectionState> getConnectionState() {
        return mConnectionStateLiveData;
    }

    public LiveData<BondingState> getBondState() {
        return mBondStateLiveData;
    }

}
