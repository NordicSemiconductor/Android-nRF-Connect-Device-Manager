/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.di.module;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import no.nordicsemi.android.mcumgr.sample.MainActivity;

@SuppressWarnings("unused")
@Module
public abstract class McuMgrActivitiesModule {
    @ContributesAndroidInjector
    abstract MainActivity contributeMainActivity();
}
