/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.sample.application.Dagger2Application;
import io.runtime.mcumgr.sample.databinding.ActivityMainBinding;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.fragment.DeviceFragment;
import io.runtime.mcumgr.sample.fragment.FilesFragment;
import io.runtime.mcumgr.sample.fragment.ImageFragment;
import io.runtime.mcumgr.sample.fragment.LogsStatsFragment;
import io.runtime.mcumgr.sample.fragment.ShellFragment;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

@SuppressWarnings("ConstantConditions")
public class MainActivity extends AppCompatActivity
        implements Injectable, HasAndroidInjector {
    public static final String EXTRA_DEVICE = "device";

    @Inject
    DispatchingAndroidInjector<Object> dispatchingAndroidInjector;
    @Inject
    McuMgrViewModelFactory viewModelFactory;
    @Inject
    McuMgrTransport mcuMgrTransport;
    @Inject
    @Nullable
    Uri logSessionUri;

    private Fragment deviceFragment;
    private Fragment imageFragment;
    private Fragment filesFragment;
    private Fragment logsStatsFragment;
    private Fragment shellFragment;

    @Override
    public AndroidInjector<Object> androidInjector() {
        return dispatchingAndroidInjector;
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        final BluetoothDevice device = getIntent().getParcelableExtra(EXTRA_DEVICE);
        // The target must be set before calling super.onCreate(Bundle).
        // Otherwise, Dagger2 will fail to inflate this Activity.
        // The target has to be set here, otherwise restoring the state will fail had the
        // Activity been destroyed and recreated.
        // Is should only be done once, when the Activity is created for the first time.
        if (savedInstanceState == null) {
            ((Dagger2Application) getApplication()).setTarget(device);
        }

        EdgeToEdge.enable(this,
                SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.light(
                        ContextCompat.getColor(this, R.color.colorSurfaceContainer),
                        ContextCompat.getColor(this, R.color.colorSurfaceContainer)
                )
        );
        super.onCreate(savedInstanceState);
        final ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                    OVERRIDE_TRANSITION_CLOSE,
                    android.R.anim.fade_in, R.anim.close
            );
        }

        String deviceName = getString(R.string.unknown_device);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            final String name = device.getName();
            if (name != null && !name.isEmpty()) {
                deviceName = name;
            }
        }
        final String deviceAddress = device.getAddress();

        // Configure the view.
        final Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(deviceName);
        getSupportActionBar().setSubtitle(deviceAddress);
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            final Insets bars = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, 0, bars.right, 0);
            return insets;
        });

        final BottomNavigationView navMenu = binding.navMenu;
        navMenu.setSelectedItemId(R.id.nav_default);
        navMenu.setOnItemSelectedListener(item -> {
            final int id = item.getItemId();
            final FragmentTransaction t = getSupportFragmentManager().beginTransaction();
            if (id == R.id.nav_default) t.show(deviceFragment); else t.hide(deviceFragment);
            if (id == R.id.nav_dfu) t.show(imageFragment); else t.hide(imageFragment);
            if (id == R.id.nav_fs) t.show(filesFragment); else t.hide(filesFragment);
            if (id == R.id.nav_stats) t.show(logsStatsFragment); else t.hide(logsStatsFragment);
            if (id == R.id.nav_shell) t.show(shellFragment); else t.hide(shellFragment);
            t.runOnCommit(this::invalidateMenu);
            t.commit();
            return true;
        });

        // Initialize fragments.
        if (savedInstanceState == null) {
            deviceFragment = new DeviceFragment();
            imageFragment = new ImageFragment();
            filesFragment = new FilesFragment();
            logsStatsFragment = new LogsStatsFragment();
            shellFragment = new ShellFragment();

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, deviceFragment, "device")
                    .add(R.id.container, imageFragment, "image")
                    .add(R.id.container, filesFragment, "fs")
                    .add(R.id.container, logsStatsFragment, "logs")
                    .add(R.id.container, shellFragment, "shell")
                    // Initially, show the Device fragment and hide others.
                    .hide(imageFragment).hide(filesFragment).hide(logsStatsFragment).hide(shellFragment)
                    .commit();
        } else {
            deviceFragment = getSupportFragmentManager().findFragmentByTag("device");
            imageFragment = getSupportFragmentManager().findFragmentByTag("image");
            filesFragment = getSupportFragmentManager().findFragmentByTag("fs");
            logsStatsFragment = getSupportFragmentManager().findFragmentByTag("logs");
            shellFragment = getSupportFragmentManager().findFragmentByTag("shell");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final @NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.log, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final @NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_open_log) {
            final Intent intent;
            if (logSessionUri == null) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=no.nordicsemi.android.log"));
            } else {
                intent = new Intent(Intent.ACTION_VIEW, logSessionUri);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isFinishing()) {
            mcuMgrTransport.release();
        }
    }
}
