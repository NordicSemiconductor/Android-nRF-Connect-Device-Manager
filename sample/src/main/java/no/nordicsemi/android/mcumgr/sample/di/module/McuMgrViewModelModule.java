/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.di.module;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import no.nordicsemi.android.mcumgr.sample.di.McuMgrScope;
import no.nordicsemi.android.mcumgr.sample.di.component.McuMgrViewModelSubComponent;
import no.nordicsemi.android.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

@Module(subcomponents = {
        McuMgrViewModelSubComponent.class
})
public class McuMgrViewModelModule {

    @Provides
    @Named("busy")
    @McuMgrScope
    @NonNull
    static MutableLiveData<Boolean> provideBusyStateLiveData() {
        return new MutableLiveData<>();
    }

    @Provides
    @McuMgrScope
    static McuMgrViewModelFactory provideMcuMgrViewModelFactory(
            final McuMgrViewModelSubComponent.Builder viewModelSubComponent
    ) {
        return new McuMgrViewModelFactory(viewModelSubComponent.build());
    }
}
