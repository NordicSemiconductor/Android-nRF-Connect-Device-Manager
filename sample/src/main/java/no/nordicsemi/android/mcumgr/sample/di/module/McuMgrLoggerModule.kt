/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.di.module

import dagger.Module
import dagger.Provides
import no.nordicsemi.android.mcumgr.log.Category
import no.nordicsemi.android.mcumgr.sample.di.McuMgrScope
import no.nordicsemi.kotlin.log.Log
import no.nordicsemi.kotlin.log.timber.Timber

@Module
class McuMgrLoggerModule {

    /**
     * The sink receiving log entries from the Mcu Manager library, that is from the managers,
     * the firmware upgrade and the transport.
     *
     * Entries are forwarded to Timber, which the application forwards to Logcat and, when
     * a log session is given, to the nRF Logger.
     */
    @Provides
    @McuMgrScope
    fun providesMcuMgrLogger(): Log.Sink<Category> = Log.Sink.Timber { _, _ -> true }
}
