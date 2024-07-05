package io.runtime.mcumgr.dfu;

import android.os.Handler;
import android.os.Looper;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

/**
 * Used to execute callbacks on the main UI thread.
 */
class MainThreadExecutor implements Executor {
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    @Override
    public void execute(@NotNull Runnable command) {
        mainThreadHandler.post(command);
    }
}