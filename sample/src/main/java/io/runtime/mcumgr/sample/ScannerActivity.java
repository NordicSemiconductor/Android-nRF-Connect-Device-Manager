/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import io.runtime.mcumgr.sample.databinding.ActivityScannerBinding;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.fragment.scanner.SavedDevicesFragment;
import io.runtime.mcumgr.sample.fragment.scanner.ScannerFragment;

public class ScannerActivity extends AppCompatActivity implements Injectable {
    // This flag is false when the app is first started (cold start).
    // In this case, the animation will be fully shown (1 sec).
    // Subsequent launches will display it only briefly.
    // It is only used on API 31+
    private static boolean coldStart = true;

    private static final String PREF_INTRO = "introShown";

    private Fragment scannerFragment;
    private Fragment savedFragment;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        // Set the proper theme for the Activity. This could have been set in "v23/styles..xml"
        // as "postSplashScreenTheme", but as this app works on pre-API-23 devices, it needs to be
        // set for them as well, and that code would not apply in such case.
        // As "postSplashScreenTheme" is optional, and setting the theme can be done using
        // setTheme, this is preferred in our case, as this also work for older platforms.
        setTheme(R.style.AppTheme);

        EdgeToEdge.enable(this,
                SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.light(
                        ContextCompat.getColor(this, R.color.colorSurfaceContainer),
                        ContextCompat.getColor(this, R.color.colorSurfaceContainer)
                )
        );
        super.onCreate(savedInstanceState);

        // Set up the splash screen.
        // The app is using SplashScreen compat library, which is supported on Android 5+, but the
        // icon is only supported on API 23+.
        //
        // See: https://android.googlesource.com/platform/frameworks/support/+/androidx-main/core/core-splashscreen/src/main/java/androidx/core/splashscreen/package-info.java
        //
        // On Android 12+ the splash screen will be animated, while on 6 - 11 will present a still
        // image. See more: https://developer.android.com/guide/topics/ui/splash-screen/
        //
        // As nRF Connect Device Manager supports Android 5+, on Android 5 and 5.1 a 9-patch image
        // is presented without the use of SplashScreen compat library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

            // Animated Vector Drawable is only supported on API 31+.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (coldStart) {
                    coldStart = false;
                    // Keep the splash screen on-screen for longer periods.
                    // Handle the splash screen transition.
                    final long then = System.currentTimeMillis();
                    splashScreen.setKeepOnScreenCondition(() -> {
                        final long now = System.currentTimeMillis();
                        return now < then + 900;
                    });
                }
            }
        }

        final ActivityScannerBinding binding = ActivityScannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Display Intro just once.
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.getBoolean(PREF_INTRO, false)) {
            preferences.edit().putBoolean(PREF_INTRO, true).apply();
            final Intent launchIntro = new Intent(this, IntroActivity.class);
            startActivity(launchIntro);
        }

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle(R.string.app_name);
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar, (v, insets) -> {
            final Insets bars = insets.getInsets(WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.navigationBars());
            v.setPadding(bars.left, 0, bars.right, 0);
            return insets;
        });

        final BottomNavigationView navMenu = binding.navMenu;
        navMenu.setSelectedItemId(R.id.nav_scanner);
        navMenu.setOnItemSelectedListener(item -> {
            final int id = item.getItemId();
            final FragmentTransaction t = getSupportFragmentManager().beginTransaction();
            if (id == R.id.nav_scanner) t.show(scannerFragment); else t.hide(scannerFragment);
            if (id == R.id.nav_bonded) t.show(savedFragment); else t.hide(savedFragment);
            t.runOnCommit(this::invalidateMenu);
            t.commit();
            return true;
        });

        // Initialize fragments.
        if (savedInstanceState == null) {
            scannerFragment = new ScannerFragment();
            savedFragment = new SavedDevicesFragment();

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, scannerFragment, "scanner")
                    .add(R.id.container, savedFragment, "saved")
                    // Initially, show the Scanner fragment and hide others.
                    .hide(savedFragment)
                    .commit();
        } else {
            scannerFragment = getSupportFragmentManager().findFragmentByTag("scanner");
            savedFragment = getSupportFragmentManager().findFragmentByTag("saved");
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        // Request POST_NOTIFICATIONS permission if not granted.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] { android.Manifest.permission.POST_NOTIFICATIONS }, 0);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.about, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.menu_about) {
            final Intent launchIntro = new Intent(this, IntroActivity.class);
            startActivity(launchIntro);
            return true;
        }
        return false;
    }
}
