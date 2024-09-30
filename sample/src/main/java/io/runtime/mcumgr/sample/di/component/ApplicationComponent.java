/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.di.component;

import javax.inject.Singleton;

import dagger.Component;
import dagger.android.AndroidInjectionModule;
import dagger.android.support.AndroidSupportInjectionModule;
import io.runtime.mcumgr.sample.application.Dagger2Application;
import io.runtime.mcumgr.sample.di.module.ActivitiesModule;
import io.runtime.mcumgr.sample.di.module.ContextModule;
import io.runtime.mcumgr.sample.di.module.FragmentsModule;
import io.runtime.mcumgr.sample.di.module.McuMgrModule;
import io.runtime.mcumgr.sample.di.module.ViewModelModule;

/**
 * Check this: https://github.com/googlesamples/android-architecture-components
 * for more details.
 */
@Component(modules = {
        AndroidInjectionModule.class,
        AndroidSupportInjectionModule.class,
        ContextModule.class,
        ActivitiesModule.class,
        FragmentsModule.class,
        ViewModelModule.class,
        McuMgrModule.class
})
@Singleton
public interface ApplicationComponent {
    @Component.Builder
    interface Builder {
        Builder contextModule(final ContextModule module);

        ApplicationComponent build();
    }

    void inject(final Dagger2Application application);
}

