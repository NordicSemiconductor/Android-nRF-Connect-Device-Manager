/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.di.module;

import android.content.SharedPreferences;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.runtime.mcumgr.sample.di.component.McuMgrSubComponent;
import io.runtime.mcumgr.sample.utils.FsUtils;
import io.runtime.mcumgr.sample.utils.ShellUtils;

@Module(subcomponents = McuMgrSubComponent.class)
public class McuMgrModule {

    @Provides
    @Singleton
    static FsUtils provideFsUtils(final SharedPreferences preferences) {
        return new FsUtils(preferences);
    }

    @Provides
    @Singleton
    static ShellUtils provideShellUtils(final SharedPreferences preferences) {
        return new ShellUtils(preferences);
    }
}
