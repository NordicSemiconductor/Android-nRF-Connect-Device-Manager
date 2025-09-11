package no.nordicsemi.android.mcumgr.sample.di.module;

import android.content.Context;

import androidx.annotation.NonNull;

import dagger.Module;
import dagger.Provides;
import no.nordicsemi.android.mcumgr.sample.di.McuMgrScope;
import no.nordicsemi.android.observability.ObservabilityManager;
import no.nordicsemi.kotlin.ble.client.android.CentralManager;
import no.nordicsemi.kotlin.ble.client.android.Peripheral;

@Module
public class ObservabilityModule {
    @Provides
    @McuMgrScope
    @NonNull
    static ObservabilityManager provideObservabilityManager(@NonNull final Context context,
                                                            @NonNull final CentralManager centralManager,
                                                            @NonNull final Peripheral peripheral) {
        final ObservabilityManager om = ObservabilityManager.create(context);
        om.connect(peripheral, centralManager);
        return om;
    }

}
