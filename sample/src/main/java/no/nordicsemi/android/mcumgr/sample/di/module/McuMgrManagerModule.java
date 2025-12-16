/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.di.module;

import dagger.Module;
import dagger.Provides;
import no.nordicsemi.android.mcumgr.McuMgrTransport;
import no.nordicsemi.android.mcumgr.dfu.mcuboot.FirmwareUpgradeManager;
import no.nordicsemi.android.mcumgr.dfu.suit.SUITUpgradeManager;
import no.nordicsemi.android.mcumgr.managers.BasicManager;
import no.nordicsemi.android.mcumgr.managers.DefaultManager;
import no.nordicsemi.android.mcumgr.managers.FsManager;
import no.nordicsemi.android.mcumgr.managers.ImageManager;
import no.nordicsemi.android.mcumgr.managers.LogManager;
import no.nordicsemi.android.mcumgr.managers.SUITManager;
import no.nordicsemi.android.mcumgr.managers.SettingsManager;
import no.nordicsemi.android.mcumgr.managers.ShellManager;
import no.nordicsemi.android.mcumgr.managers.StatsManager;
import no.nordicsemi.android.mcumgr.sample.di.McuMgrScope;
import no.nordicsemi.android.ota.mcumgr.MemfaultManager;

@Module
public class McuMgrManagerModule {

    @Provides
    @McuMgrScope
    static SettingsManager provideConfigManager(final McuMgrTransport transport) {
        return new SettingsManager(transport);
    }

    @Provides
    @McuMgrScope
    static DefaultManager provideDefaultManager(final McuMgrTransport transport) {
        return new DefaultManager(transport);
    }

    @Provides
    @McuMgrScope
    static FsManager provideFsManager(final McuMgrTransport transport) {
        return new FsManager(transport);
    }

    @Provides
    @McuMgrScope
    static LogManager provideLogManager(final McuMgrTransport transport) {
        return new LogManager(transport);
    }

    @Provides
    @McuMgrScope
    static ImageManager provideImageManager(final McuMgrTransport transport) {
        return new ImageManager(transport);
    }

    @Provides
    @McuMgrScope
    static BasicManager provideBasicManager(final McuMgrTransport transport) {
        return new BasicManager(transport);
    }

    @Provides
    @McuMgrScope
    static StatsManager provideStatsManager(final McuMgrTransport transport) {
        return new StatsManager(transport);
    }

    @Provides
    @McuMgrScope
    static ShellManager provideShellManager(final McuMgrTransport transport) {
        return new ShellManager(transport);
    }

    @Provides
    @McuMgrScope
    static FirmwareUpgradeManager provideFirmwareUpgradeManager(final McuMgrTransport transport) {
        return new FirmwareUpgradeManager(transport);
    }

    @Provides
    @McuMgrScope
    static SUITManager provideSUITManager(final McuMgrTransport transport) {
        return new SUITManager(transport);
    }

    @Provides
    @McuMgrScope
    static SUITUpgradeManager provideSUITUpgradeManager(final McuMgrTransport transport) {
        return new SUITUpgradeManager(transport);
    }

    @Provides
    @McuMgrScope
    static MemfaultManager provideMemfaultManager(final McuMgrTransport transport) {
        return new MemfaultManager(transport);
    }
}
