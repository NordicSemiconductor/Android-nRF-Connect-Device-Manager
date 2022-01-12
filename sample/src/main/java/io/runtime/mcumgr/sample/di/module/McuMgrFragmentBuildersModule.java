/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.di.module;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import io.runtime.mcumgr.sample.di.McuMgrScope;
import io.runtime.mcumgr.sample.dialog.PartitionDialogFragment;
import io.runtime.mcumgr.sample.fragment.ImageFragment;
import io.runtime.mcumgr.sample.fragment.mcumgr.DeviceStatusFragment;
import io.runtime.mcumgr.sample.fragment.mcumgr.EchoFragment;
import io.runtime.mcumgr.sample.fragment.mcumgr.FilesDownloadFragment;
import io.runtime.mcumgr.sample.fragment.mcumgr.FilesUploadFragment;
import io.runtime.mcumgr.sample.fragment.mcumgr.ImageControlFragment;
import io.runtime.mcumgr.sample.fragment.mcumgr.ImageSettingsFragment;
import io.runtime.mcumgr.sample.fragment.mcumgr.ImageUpgradeFragment;
import io.runtime.mcumgr.sample.fragment.mcumgr.ImageUploadFragment;
import io.runtime.mcumgr.sample.fragment.mcumgr.ResetFragment;
import io.runtime.mcumgr.sample.fragment.mcumgr.StatsFragment;

@SuppressWarnings("unused")
@Module
public abstract class McuMgrFragmentBuildersModule {
    @McuMgrScope
    @ContributesAndroidInjector
    abstract DeviceStatusFragment contributeDeviceStatusFragment();

    @McuMgrScope
    @ContributesAndroidInjector
    abstract EchoFragment contributeEchoFragment();

    @McuMgrScope
    @ContributesAndroidInjector
    abstract ResetFragment contributeResetFragment();

    @McuMgrScope
    @ContributesAndroidInjector
    abstract StatsFragment contributeStatsFragment();

    @McuMgrScope
    @ContributesAndroidInjector
    abstract ImageFragment contributeImageFragment();

    @McuMgrScope
    @ContributesAndroidInjector
    abstract ImageUpgradeFragment contributeImageUpgradeFragment();

    @McuMgrScope
    @ContributesAndroidInjector
    abstract ImageUploadFragment contributeImageUploadFragment();

    @McuMgrScope
    @ContributesAndroidInjector
    abstract ImageControlFragment contributeImageControlFragment();

    @McuMgrScope
    @ContributesAndroidInjector
    abstract ImageSettingsFragment contributeImageSettingsFragment();

    @McuMgrScope
    @ContributesAndroidInjector
    abstract PartitionDialogFragment contributePartitionDialogFragment();

    @McuMgrScope
    @ContributesAndroidInjector
    abstract FilesDownloadFragment contributeFileDownloadFragment();

    @McuMgrScope
    @ContributesAndroidInjector
    abstract FilesUploadFragment contributeFilesUploadFragment();
}
