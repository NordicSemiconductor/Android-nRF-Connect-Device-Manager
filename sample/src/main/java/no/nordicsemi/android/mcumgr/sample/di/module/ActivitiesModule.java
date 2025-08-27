/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.di.module;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import no.nordicsemi.android.mcumgr.sample.ScannerActivity;

@SuppressWarnings("unused")
@Module
public abstract class ActivitiesModule {
    @ContributesAndroidInjector
    abstract ScannerActivity contributeScannerActivity();
}
