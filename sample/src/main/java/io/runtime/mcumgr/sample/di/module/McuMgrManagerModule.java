/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.di.module;

import dagger.Module;
import dagger.Provides;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;
import io.runtime.mcumgr.managers.BasicManager;
import io.runtime.mcumgr.managers.ConfigManager;
import io.runtime.mcumgr.managers.DefaultManager;
import io.runtime.mcumgr.managers.FsManager;
import io.runtime.mcumgr.managers.ImageManager;
import io.runtime.mcumgr.managers.LogManager;
import io.runtime.mcumgr.managers.StatsManager;
import io.runtime.mcumgr.sample.di.McuMgrScope;

@Module
public class McuMgrManagerModule {

    @Provides
    @McuMgrScope
    static ConfigManager provideConfigManager(final McuMgrTransport transport) {
        return new ConfigManager(transport);
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
    static FirmwareUpgradeManager provideFirmwareUpgradeManager(final McuMgrTransport transport) {
        return new FirmwareUpgradeManager(transport);
    }
}
