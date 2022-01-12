package io.runtime.mcumgr.sample.viewmodel;

import android.app.Application;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import javax.inject.Inject;

import io.runtime.mcumgr.McuMgrTransport;

public class MainViewModel extends AndroidViewModel {
    @Inject
    McuMgrTransport mcuMgrTransport;
    @Inject
    HandlerThread handlerThread;

    @Inject
    public MainViewModel(@NonNull final Application application) {
        super(application);
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        mcuMgrTransport.release();
    }
}
