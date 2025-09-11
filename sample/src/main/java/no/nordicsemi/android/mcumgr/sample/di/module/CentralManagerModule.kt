package no.nordicsemi.android.mcumgr.sample.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.native
import javax.inject.Singleton

@Module
class CentralManagerModule {

    @Provides
    @Singleton
    fun providesIoScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    @Provides
    @Singleton
    fun providesCentralManager(context: Context, scope: CoroutineScope): CentralManager {
        return CentralManager.Factory.native(context, scope)
    }
}