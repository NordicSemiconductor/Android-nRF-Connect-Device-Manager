/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.di.component;

import android.bluetooth.BluetoothDevice;

import dagger.BindsInstance;
import dagger.Subcomponent;
import io.runtime.mcumgr.sample.application.Dagger2Application;
import io.runtime.mcumgr.sample.di.McuMgrScope;
import io.runtime.mcumgr.sample.di.module.McuMgrActivitiesModule;
import io.runtime.mcumgr.sample.di.module.McuMgrFragmentBuildersModule;
import io.runtime.mcumgr.sample.di.module.McuMgrManagerModule;
import io.runtime.mcumgr.sample.di.module.McuMgrTransportModule;
import io.runtime.mcumgr.sample.di.module.McuMgrViewModelModule;

@Subcomponent(modules = {
        McuMgrActivitiesModule.class,
        McuMgrFragmentBuildersModule.class,
        McuMgrViewModelModule.class,
        McuMgrTransportModule.class,
        McuMgrManagerModule.class
})
@McuMgrScope
public interface McuMgrSubComponent {
    @Subcomponent.Builder
    interface Builder {
        /**
         * Sets the connection target.
         *
         * @param device teh target Bluetooth device.
         * @return The builder instance.
         */
        @BindsInstance
        Builder target(final BluetoothDevice device);

        McuMgrSubComponent build();
    }

    /**
     * Adds the {@link io.runtime.mcumgr.sample.MainActivity} to the
     * {@link Dagger2Application#dispatchingAndroidInjector}.
     * The {@link io.runtime.mcumgr.sample.MainActivity} requires the
     * {@link io.runtime.mcumgr.McuMgrTransport} to be instantiated before injecting.
     *
     * @param application the application.
     */
    void update(final Dagger2Application application);
}
