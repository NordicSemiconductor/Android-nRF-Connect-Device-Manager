/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.di.component;

import dagger.Subcomponent;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.DeviceStatusViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.FilesDownloadViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.FilesUploadViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.ImageUpgradeViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.ImageUploadViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.EchoViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.ImageControlViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.ResetViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.StatsViewModel;

/**
 * A sub component to create ViewModels. It is called by the
 * {@link McuMgrViewModelFactory}. Using this component allows
 * ViewModels to define {@link javax.inject.Inject} constructors.
 */
@Subcomponent
public interface McuMgrViewModelSubComponent {
	@Subcomponent.Builder
	interface Builder {
		McuMgrViewModelSubComponent build();
	}
	DeviceStatusViewModel deviceStatusViewModel();
	EchoViewModel echoViewModel();
	ResetViewModel resetViewModel();
	StatsViewModel statsViewModel();
	McuMgrViewModel mcuMgrViewModel();
	ImageUpgradeViewModel imageUpgradeViewModel();
	ImageUploadViewModel imageUploadViewModel();
	ImageControlViewModel imageControlViewModel();
	FilesDownloadViewModel filesDownloadViewModel();
	FilesUploadViewModel filesUploadViewModel();
}
