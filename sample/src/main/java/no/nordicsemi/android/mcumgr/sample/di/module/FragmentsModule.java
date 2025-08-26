/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.di.module;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import no.nordicsemi.android.mcumgr.sample.fragment.scanner.SavedDevicesFragment;
import no.nordicsemi.android.mcumgr.sample.fragment.scanner.ScannerFragment;

@SuppressWarnings("unused")
@Module
public abstract class FragmentsModule {
    @ContributesAndroidInjector
    abstract ScannerFragment contributeScannerFragment();
    @ContributesAndroidInjector
    abstract SavedDevicesFragment contributeSavedDevicesFragment();
}
