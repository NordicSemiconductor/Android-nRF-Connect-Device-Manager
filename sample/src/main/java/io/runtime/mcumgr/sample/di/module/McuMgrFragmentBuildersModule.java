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
    @ContributesAndroidInjector
    abstract DeviceStatusFragment contributeDeviceStatusFragment();
    @ContributesAndroidInjector
    abstract EchoFragment contributeEchoFragment();
    @ContributesAndroidInjector
    abstract ResetFragment contributeResetFragment();
    @ContributesAndroidInjector
    abstract StatsFragment contributeStatsFragment();
    @ContributesAndroidInjector
    abstract ImageFragment contributeImageFragment();
    @ContributesAndroidInjector
    abstract ImageUpgradeFragment contributeImageUpgradeFragment();
    @ContributesAndroidInjector
    abstract ImageUploadFragment contributeImageUploadFragment();
    @ContributesAndroidInjector
    abstract ImageControlFragment contributeImageControlFragment();
    @ContributesAndroidInjector
    abstract ImageSettingsFragment contributeImageSettingsFragment();
    @ContributesAndroidInjector
    abstract PartitionDialogFragment contributePartitionDialogFragment();
    @ContributesAndroidInjector
    abstract FilesDownloadFragment contributeFileDownloadFragment();
    @ContributesAndroidInjector
    abstract FilesUploadFragment contributeFilesUploadFragment();
}
