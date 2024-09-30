/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.di.component;

import dagger.Subcomponent;
import io.runtime.mcumgr.sample.viewmodel.scanner.SavedDevicesViewModel;
import io.runtime.mcumgr.sample.viewmodel.scanner.ScannerViewModel;
import io.runtime.mcumgr.sample.viewmodel.ViewModelFactory;

/**
 * A sub component to create ViewModels. It is called by the
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
