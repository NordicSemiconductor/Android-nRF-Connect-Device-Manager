/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.di.module;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import no.nordicsemi.android.mcumgr.sample.dialog.PartitionDialogFragment;
import no.nordicsemi.android.mcumgr.sample.fragment.ImageFragment;
import no.nordicsemi.android.mcumgr.sample.fragment.mcumgr.DeviceStatusFragment;
import no.nordicsemi.android.mcumgr.sample.fragment.mcumgr.EchoFragment;
import no.nordicsemi.android.mcumgr.sample.fragment.mcumgr.ExecFragment;
import no.nordicsemi.android.mcumgr.sample.fragment.mcumgr.FilesDownloadFragment;
import no.nordicsemi.android.mcumgr.sample.fragment.mcumgr.FilesUploadFragment;
import no.nordicsemi.android.mcumgr.sample.fragment.mcumgr.ImageControlFragment;
import no.nordicsemi.android.mcumgr.sample.fragment.mcumgr.ImageSettingsFragment;
import no.nordicsemi.android.mcumgr.sample.fragment.mcumgr.ImageUpgradeFragment;
import no.nordicsemi.android.mcumgr.sample.fragment.mcumgr.ImageUploadFragment;
import no.nordicsemi.android.mcumgr.sample.fragment.mcumgr.ObservabilityFragment;
import no.nordicsemi.android.mcumgr.sample.fragment.mcumgr.ResetFragment;
import no.nordicsemi.android.mcumgr.sample.fragment.mcumgr.StatsFragment;

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
    abstract ObservabilityFragment contributeObservabilityFragment();
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
    @ContributesAndroidInjector
    abstract ExecFragment contributeExecFragment();
}
