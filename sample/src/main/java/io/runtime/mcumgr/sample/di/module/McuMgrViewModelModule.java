/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.di.module;

import dagger.Module;
import dagger.Provides;
import io.runtime.mcumgr.sample.di.McuMgrScope;
import io.runtime.mcumgr.sample.di.component.McuMgrViewModelSubComponent;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

@Module(subcomponents = {
		McuMgrViewModelSubComponent.class
})
public class McuMgrViewModelModule {

	@Provides
	@McuMgrScope
	static McuMgrViewModelFactory provideMcuMgrViewModelFactory(
			final McuMgrViewModelSubComponent.Builder viewModelSubComponent) {
		return new McuMgrViewModelFactory(viewModelSubComponent.build());
	}
}
