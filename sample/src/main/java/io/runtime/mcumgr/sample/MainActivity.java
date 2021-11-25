/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.fragment.DeviceFragment;
import io.runtime.mcumgr.sample.fragment.FilesFragment;
import io.runtime.mcumgr.sample.fragment.ImageFragment;
import io.runtime.mcumgr.sample.fragment.LogsStatsFragment;
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

    @Override
    public AndroidInjector<Object> androidInjector() {
        return dispatchingAndroidInjector;
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final BluetoothDevice device = getIntent().getParcelableExtra(EXTRA_DEVICE);
        final String deviceName = device.getName();
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
            switch (item.getItemId()) {
                case R.id.nav_default:
                    getSupportFragmentManager().beginTransaction()
                            .show(deviceFragment).hide(imageFragment)
                            .hide(filesFragment).hide(logsStatsFragment)
                            .commit();
                    return true;
                case R.id.nav_dfu:
                    getSupportFragmentManager().beginTransaction()
                            .hide(deviceFragment).show(imageFragment)
                            .hide(filesFragment).hide(logsStatsFragment)
                            .commit();
                    return true;
                case R.id.nav_fs:
                    getSupportFragmentManager().beginTransaction()
                            .hide(deviceFragment).hide(imageFragment)
                            .show(filesFragment).hide(logsStatsFragment)
                            .commit();
                    return true;
                case R.id.nav_stats:
                    getSupportFragmentManager().beginTransaction()
                            .hide(deviceFragment).hide(imageFragment)
                            .hide(filesFragment).show(logsStatsFragment)
                            .commit();
                    return true;
            }
            return false;
        });

        // Initialize fragments.
        if (savedInstanceState == null) {
            deviceFragment = new DeviceFragment();
            imageFragment = new ImageFragment();
            filesFragment = new FilesFragment();
            logsStatsFragment = new LogsStatsFragment();

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, deviceFragment, "device")
                    .add(R.id.container, imageFragment, "image")
                    .add(R.id.container, filesFragment, "fs")
                    .add(R.id.container, logsStatsFragment, "logs")
                    // Initially, show the Device fragment and hide others.
                    .hide(imageFragment).hide(filesFragment).hide(logsStatsFragment)
                    .commit();
        } else {
            deviceFragment = getSupportFragmentManager().findFragmentByTag("device");
            imageFragment = getSupportFragmentManager().findFragmentByTag("image");
            filesFragment = getSupportFragmentManager().findFragmentByTag("fs");
            logsStatsFragment = getSupportFragmentManager().findFragmentByTag("logs");
        }
    }
}
