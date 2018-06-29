/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.di;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import dagger.android.AndroidInjection;
import dagger.android.support.AndroidSupportInjection;
import dagger.android.support.HasSupportFragmentInjector;
import io.runtime.mcumgr.sample.application.Dagger2Application;
import io.runtime.mcumgr.sample.di.component.DaggerApplicationComponent;
import io.runtime.mcumgr.sample.di.module.ContextModule;

/**
 * Helper class to automatically inject fragments if they implement {@link Injectable}.
 */
public class AppInjector {
	private AppInjector() {
	}

	public static void init(final Dagger2Application application) {
		DaggerApplicationComponent.builder()
				.contextModule(new ContextModule(application))
				.build()
				.inject(application);

		application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
			@Override
			public void onActivityCreated(final Activity activity, final Bundle savedInstanceState) {
				handleActivity(activity);
			}

			@Override
			public void onActivityStarted(final Activity activity) {
				// empty
			}

			@Override
			public void onActivityResumed(final Activity activity) {
				// empty
			}

			@Override
			public void onActivityPaused(final Activity activity) {
				// empty
			}

			@Override
			public void onActivityStopped(final Activity activity) {
				// empty
			}

			@Override
			public void onActivitySaveInstanceState(final Activity activity, final Bundle outState) {
				// empty
			}

			@Override
			public void onActivityDestroyed(final Activity activity) {
				// empty
			}
		});
	}

	private static void handleActivity(final Activity activity) {
		if (activity instanceof Injectable || activity instanceof HasSupportFragmentInjector) {
			AndroidInjection.inject(activity);
		}
		if (activity instanceof AppCompatActivity) {
			((AppCompatActivity) activity).getSupportFragmentManager()
					.registerFragmentLifecycleCallbacks(
					new FragmentManager.FragmentLifecycleCallbacks() {
						@Override
						public void onFragmentPreCreated(final FragmentManager fm, final Fragment f, final Bundle savedInstanceState) {
							if (f instanceof Injectable) {
								AndroidSupportInjection.inject(f);
							}
						}
					}, true);
		}
	}
}
