/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.di.module;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import io.runtime.mcumgr.sample.MainActivity;
import io.runtime.mcumgr.sample.di.McuMgrScope;

@SuppressWarnings("unused")
@Module
public abstract class McuMgrActivitiesModule {
    @McuMgrScope
    @ContributesAndroidInjector
    abstract MainActivity contributeMainActivity();
}
