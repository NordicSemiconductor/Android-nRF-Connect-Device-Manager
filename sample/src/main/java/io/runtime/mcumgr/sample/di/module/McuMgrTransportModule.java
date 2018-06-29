/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.di.module;

import android.arch.lifecycle.MutableLiveData;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.ble.McuMgrBleTransport;
import io.runtime.mcumgr.sample.di.McuMgrScope;

@Module
public class McuMgrTransportModule {

	@Provides
	@Named("busy")
	@McuMgrScope
	static MutableLiveData<Boolean> provideBusyStateLiveData() {
		return new MutableLiveData<>();
	}

	@Provides
	@McuMgrScope
	static McuMgrTransport provideMcuMgrTransport(@NonNull final Context context,
												  @NonNull final BluetoothDevice device) {
		return new McuMgrBleTransport(context, device);
	}
}
