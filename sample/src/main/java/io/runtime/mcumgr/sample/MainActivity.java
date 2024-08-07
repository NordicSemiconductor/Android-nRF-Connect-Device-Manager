/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.fragment.DeviceFragment;
import io.runtime.mcumgr.sample.fragment.FilesFragment;
import io.runtime.mcumgr.sample.fragment.ImageFragment;
import io.runtime.mcumgr.sample.fragment.LogsStatsFragment;
import io.runtime.mcumgr.sample.fragment.ShellFragment;
import io.runtime.mcumgr.sample.viewmodel.MainViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

@SuppressWarnings("ConstantConditions")
public class MainActivity extends AppCompatActivity
        implements Injectable, HasAndroidInjector {
    public static final String EXTRA_DEVICE = "device";

    @Inject
    DispatchingAndroidInjector<Object> dispatchingAndroidInjector;
    @Inject
    McuMgrViewModelFactory viewModelFactory;

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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                    OVERRIDE_TRANSITION_CLOSE,
                    0, R.anim.transitions
            );
        }

        final BluetoothDevice device = getIntent().getParcelableExtra(EXTRA_DEVICE);
        String deviceName = getString(R.string.unknown_device);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            final String name = device.getName();
            if (name != null && !name.isEmpty()) {
                deviceName = name;
            }
        }
        final String deviceAddress = device.getAddress();

        new ViewModelProvider(this, viewModelFactory)
                .get(MainViewModel.class);

        // Configure the view.
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(deviceName);
        getSupportActionBar().setSubtitle(deviceAddress);

        final BottomNavigationView navMenu = findViewById(R.id.nav_menu);
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
}
