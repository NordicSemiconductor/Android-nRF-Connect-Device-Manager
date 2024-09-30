/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.di.module;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import io.runtime.mcumgr.sample.fragment.scanner.SavedDevicesFragment;
import io.runtime.mcumgr.sample.fragment.scanner.ScannerFragment;

@SuppressWarnings("unused")
@Module
public abstract class FragmentsModule {
    @ContributesAndroidInjector
    abstract ScannerFragment contributeScannerFragment();
    @ContributesAndroidInjector
    abstract SavedDevicesFragment contributeSavedDevicesFragment();
}
