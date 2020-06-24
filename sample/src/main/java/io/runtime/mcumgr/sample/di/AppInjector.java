/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.di;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import org.jetbrains.annotations.NotNull;

import dagger.android.AndroidInjection;
import dagger.android.HasAndroidInjector;
import dagger.android.support.AndroidSupportInjection;
import io.runtime.mcumgr.sample.application.Dagger2Application;
import io.runtime.mcumgr.sample.di.component.DaggerApplicationComponent;
import io.runtime.mcumgr.sample.di.module.ContextModule;

/**
 * Helper class to automatically inject fragments if they implement {@link Injectable}.
 */
public class AppInjector {
    private AppInjector() {
    }

    public static void init(@NonNull final Dagger2Application application) {
        DaggerApplicationComponent.builder()
                .contextModule(new ContextModule(application))
                .build()
                .inject(application);

        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NotNull final Activity activity,
                                          @Nullable final Bundle savedInstanceState) {
                handleActivity(activity);
            }

            @Override
            public void onActivityStarted(@NotNull final Activity activity) {
                // empty
            }

            @Override
            public void onActivityResumed(@NotNull final Activity activity) {
                // empty
            }

            @Override
            public void onActivityPaused(@NotNull final Activity activity) {
                // empty
            }

            @Override
            public void onActivityStopped(@NotNull final Activity activity) {
                // empty
            }

            @Override
            public void onActivitySaveInstanceState(@NotNull final Activity activity,
                                                    @NotNull final Bundle outState) {
                // empty
            }

            @Override
            public void onActivityDestroyed(@NotNull final Activity activity) {
                // empty
            }
        });
    }

    private static void handleActivity(@NonNull final Activity activity) {
        if (activity instanceof Injectable || activity instanceof HasAndroidInjector) {
            AndroidInjection.inject(activity);
        }
        if (activity instanceof FragmentActivity) {
            ((AppCompatActivity) activity).getSupportFragmentManager()
                    .registerFragmentLifecycleCallbacks(
                            new FragmentManager.FragmentLifecycleCallbacks() {
                                @Override
                                public void onFragmentPreCreated(@NonNull final FragmentManager fm,
                                                                 @NonNull final Fragment f,
                                                                 final Bundle savedInstanceState) {
                                    if (f instanceof Injectable) {
                                        AndroidSupportInjection.inject(f);
                                    }
                                }
                            }, true);
        }
    }
}
