/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.di.component;

import dagger.Subcomponent;
import no.nordicsemi.android.mcumgr.sample.viewmodel.ViewModelFactory;
import no.nordicsemi.android.mcumgr.sample.viewmodel.scanner.SavedDevicesViewModel;
import no.nordicsemi.android.mcumgr.sample.viewmodel.scanner.ScannerViewModel;

/**
 * A subcomponent to create ViewModels. It is called by the
 * {@link ViewModelFactory}. Using this component allows
 * ViewModels to define {@link javax.inject.Inject} constructors.
 */
@Subcomponent
public interface ViewModelSubComponent {
    @Subcomponent.Builder
    interface Builder {
        ViewModelSubComponent build();
    }

    ScannerViewModel scannerViewModel();
    SavedDevicesViewModel savedDevicesViewModel();
}
