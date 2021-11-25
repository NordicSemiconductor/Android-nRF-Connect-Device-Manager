package io.runtime.mcumgr.sample.viewmodel;

import android.app.Application;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import javax.inject.Inject;

import io.runtime.mcumgr.McuMgrTransport;

public class MainViewModel extends AndroidViewModel {
    @Inject
    McuMgrTransport mMcuMgrTransport;
    @Inject
    HandlerThread mHandlerThread;

    @Inject
    public MainViewModel(@NonNull final Application application) {
        super(application);
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        mMcuMgrTransport.release();
        mHandlerThread.quitSafely();
    }
}
