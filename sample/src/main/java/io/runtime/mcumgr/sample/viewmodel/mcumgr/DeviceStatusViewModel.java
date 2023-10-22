/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.viewmodel.mcumgr;

import org.jetbrains.annotations.NotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import io.runtime.mcumgr.response.dflt.McuMgrAppInfoResponse;
import io.runtime.mcumgr.response.dflt.McuMgrBootloaderInfoResponse;
import io.runtime.mcumgr.response.dflt.McuMgrParamsResponse;
import io.runtime.mcumgr.sample.observable.BondingState;
import io.runtime.mcumgr.sample.observable.ConnectionState;
import io.runtime.mcumgr.sample.observable.ObservableMcuMgrBleTransport;

public class DeviceStatusViewModel extends McuMgrViewModel {
    private final LiveData<ConnectionState> connectionStateLiveData;
    private final LiveData<BondingState> bondStateLiveData;

    private final MutableLiveData<McuMgrBufferParams> bufferLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> bootloaderNameLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> bootloaderModeLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> appInfoLiveData = new MutableLiveData<>();
    private final Observer<ConnectionState> connectionStateObserver = connectionState -> {
        if (connectionState == ConnectionState.READY) {
            // Read sequentially:
            // 1. MCU Manager parameters
            // 2. Application info (parameter: "sv" will return the kernel name and version)
            // 3. Bootloader name
            // and, if the bootloader is "MCUboot":
            // 4. Bootloader mode
            readMcuMgrParams(() -> readAppInfo("sv", () -> readBootloaderName((name) -> {
                if ("MCUboot".equals(name)) {
                    readMcuBootMode(null);
                }
            })));
        } else {
            bufferLiveData.postValue(null);
            bootloaderModeLiveData.postValue(null);
            bootloaderNameLiveData.postValue(null);
            appInfoLiveData.postValue(null);
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

    public LiveData<String> getBootloaderName() { return bootloaderNameLiveData; }

    public LiveData<Integer> getBootloaderMode() { return bootloaderModeLiveData; }

    public LiveData<String> getAppInfo() { return appInfoLiveData; }

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

    /**
     * Reads the MCU Manager parameters.
     *
     * @param then a callback to be invoked when the parameters are read.
     */
    private void readMcuMgrParams(@Nullable final Runnable then) {
        defaultManager.params(new McuMgrCallback<>() {
            @Override
            public void onResponse(@NotNull final McuMgrParamsResponse response) {
                bufferLiveData.postValue(new McuMgrBufferParams(response));
                if (then != null) {
                    then.run();
                }
            }

            @Override
            public void onError(@NotNull final McuMgrException error) {
                final McuMgrTransport transport = defaultManager.getTransporter();
                if (transport instanceof final McuMgrBleTransport bleTransport) {
                    final int maxPacketLength = bleTransport.getMaxPacketLength();
                    final McuMgrBufferParams mcuParams = new McuMgrBufferParams(maxPacketLength);
                    bufferLiveData.postValue(mcuParams);
                } else {
                    bufferLiveData.postValue(null);
                }
                if (then != null) {
                    then.run();
                }
            }
        });
    }

    /**
     * Reads application info.
     *
     * @param format See {@link DefaultManager#appInfo(String)} for details.
     * @noinspection SameParameterValue
     */
    private void readAppInfo(@Nullable final String format, @Nullable final Runnable then) {
        defaultManager.appInfo(format, new McuMgrCallback<>() {
            @Override
            public void onResponse(@NotNull McuMgrAppInfoResponse response) {
                appInfoLiveData.postValue(response.output);
                if (then != null) {
                    then.run();
                }
            }

            @Override
            public void onError(@NotNull McuMgrException error) {
                appInfoLiveData.postValue(null);
                if (then != null) {
                    then.run();
                }
            }
        });
    }

    /**
     * A callback to be invoked when the bootloader name is read.
     */
    private interface BootloaderNameCallback {
        void onBootloaderNameReceived(@NonNull String bootloaderName);
    }

    /**
     * Reads the name of the bootloader.
     *
     * @param then a callback to be invoked when the name is read.
     */
    private void readBootloaderName(@Nullable final BootloaderNameCallback then) {
        defaultManager.bootloaderInfo(DefaultManager.BOOTLOADER_INFO_QUERY_BOOTLOADER, new McuMgrCallback<>() {
            @Override
            public void onResponse(@NotNull McuMgrBootloaderInfoResponse response) {
                bootloaderNameLiveData.postValue(response.bootloader);
                if (then != null) {
                    then.onBootloaderNameReceived(response.bootloader);
                }
            }

            @Override
            public void onError(@NotNull McuMgrException error) {
                bootloaderNameLiveData.postValue(null);
            }
        });
    }

    /**
     * Reads the mode of the bootloader.
     * This method is only supported by MCUboot bootloader.
     *
     * @param then a callback to be invoked when the mode is read.
     * @noinspection SameParameterValue
     */
    private void readMcuBootMode(@Nullable final Runnable then) {
        defaultManager.bootloaderInfo(DefaultManager.BOOTLOADER_INFO_MCUBOOT_QUERY_MODE, new McuMgrCallback<>() {
            @Override
            public void onResponse(@NotNull McuMgrBootloaderInfoResponse response) {
                bootloaderModeLiveData.postValue(response.mode);
                if (then != null) {
                    then.run();
                }
            }

            @Override
            public void onError(@NotNull McuMgrException error) {
                bootloaderModeLiveData.postValue(null);
                if (then != null) {
                    then.run();
                }
            }
        });
    }
}
