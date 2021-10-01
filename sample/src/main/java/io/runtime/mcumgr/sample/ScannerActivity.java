/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import javax.inject.Inject;

import io.runtime.mcumgr.sample.adapter.DevicesAdapter;
import io.runtime.mcumgr.sample.databinding.ActivityScannerBinding;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.utils.Utils;
import io.runtime.mcumgr.sample.viewmodel.ScannerStateLiveData;
import io.runtime.mcumgr.sample.viewmodel.ScannerViewModel;
import io.runtime.mcumgr.sample.viewmodel.ViewModelFactory;

public class ScannerActivity extends AppCompatActivity
        implements Injectable, DevicesAdapter.OnItemClickListener {
    private static final String PREF_INTRO = "introShown";

    @Inject
    ViewModelFactory mViewModelFactory;

    private ActivityScannerBinding mBinding;

    private ScannerViewModel mScannerViewModel;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityScannerBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        // Display Intro just once.
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.getBoolean(PREF_INTRO, false)) {
            preferences.edit().putBoolean(PREF_INTRO, true).apply();
            final Intent launchIntro = new Intent(this, IntroActivity.class);
            startActivity(launchIntro);
        }

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);

        // Create view model containing utility methods for scanning
        mScannerViewModel = new ViewModelProvider(this, mViewModelFactory)
                .get(ScannerViewModel.class);
        mScannerViewModel.getScannerState().observe(this, this::startScan);

        // Configure the recycler view
        final RecyclerView recyclerView = findViewById(R.id.recycler_view_ble_devices);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        final DividerItemDecoration dividerItemDecoration =
                new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        final DevicesAdapter adapter =
                new DevicesAdapter(this, mScannerViewModel.getDevices());
        adapter.setOnItemClickListener(this);
        recyclerView.setAdapter(adapter);

        // Set up permission request launcher
        final ActivityResultLauncher<String> requestPermission =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                        result -> mScannerViewModel.refresh()
        );
        final ActivityResultLauncher<String[]> requestPermissions =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                        result -> mScannerViewModel.refresh()
                );

        // Configure views
        mBinding.noDevices.actionEnableLocation.setOnClickListener(v -> openLocationSettings());
        mBinding.bluetoothOff.actionEnableBluetooth.setOnClickListener(v -> requestBluetoothEnabled());
        mBinding.noLocationPermission.actionGrantLocationPermission.setOnClickListener(v -> {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION))
                Utils.markLocationPermissionRequested(this);
            requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        });
        mBinding.noLocationPermission.actionPermissionSettings.setOnClickListener(v -> {
            Utils.clearLocationPermissionRequested(this);
            openPermissionSettings();
        });
        if (Utils.isSorAbove()) {
            mBinding.noBluetoothPermission.actionGrantBluetoothPermission.setOnClickListener(v -> {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.BLUETOOTH_SCAN)) {
                    Utils.markBluetoothScanPermissionRequested(this);
                }
                requestPermissions.launch(new String[] {
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                });
            });
            mBinding.noBluetoothPermission.actionPermissionSettings.setOnClickListener(v -> {
                Utils.clearBluetoothPermissionRequested(this);
                openPermissionSettings();
            });
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mScannerViewModel.getDevices().clear();
        mScannerViewModel.getScannerState().clearRecords();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mScannerViewModel.stopScan();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull final Menu menu) {
        getMenuInflater().inflate(R.menu.filter, menu);
        getMenuInflater().inflate(R.menu.about, menu);
        menu.findItem(R.id.filter_uuid).setChecked(mScannerViewModel.isUuidFilterEnabled());
        menu.findItem(R.id.filter_nearby).setChecked(mScannerViewModel.isNearbyFilterEnabled());
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.filter_uuid:
                item.setChecked(!item.isChecked());
                mScannerViewModel.filterByUuid(item.isChecked());
                return true;
            case R.id.filter_nearby:
                item.setChecked(!item.isChecked());
                mScannerViewModel.filterByDistance(item.isChecked());
                return true;
            case R.id.menu_about:
                final Intent launchIntro = new Intent(this, IntroActivity.class);
                startActivity(launchIntro);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(@NonNull final BluetoothDevice device) {
        final Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_DEVICE, device);
        startActivity(intent);
    }

    /**
     * Start scanning for Bluetooth devices or displays a message based on the scanner state.
     */
    private void startScan(final ScannerStateLiveData state) {
        // First, check the Location permission.
        // This is required since Marshmallow up until Android 11 in order to scan for Bluetooth LE
        // devices.
        if (!Utils.isLocationPermissionRequired() ||
                Utils.isLocationPermissionGranted(this)) {
            mBinding.noLocationPermission.getRoot().setVisibility(View.GONE);

            // Bluetooth must be enabled
            if (state.isBluetoothEnabled()) {
                mBinding.bluetoothOff.getRoot().setVisibility(View.GONE);

                if (!Utils.isSorAbove() || Utils.isBluetoothScanPermissionGranted(this)) {
                    mBinding.noBluetoothPermission.getRoot().setVisibility(View.GONE);

                    // We are now OK to start scanning
                    mScannerViewModel.startScan();
                    mBinding.progressBar.setVisibility(View.VISIBLE);

                    if (!state.hasRecords()) {
                        mBinding.noDevices.getRoot().setVisibility(View.VISIBLE);

                        if (!Utils.isLocationRequired(this) ||
                                Utils.isLocationEnabled(this)) {
                            mBinding.noDevices.noLocation.setVisibility(View.INVISIBLE);
                        } else {
                            mBinding.noDevices.noLocation.setVisibility(View.VISIBLE);
                        }
                    } else {
                        mBinding.noDevices.getRoot().setVisibility(View.GONE);
                    }
                } else {
                    mBinding.noBluetoothPermission.getRoot().setVisibility(View.VISIBLE);
                    mBinding.bluetoothOff.getRoot().setVisibility(View.GONE);
                    mBinding.progressBar.setVisibility(View.INVISIBLE);
                    mBinding.noDevices.getRoot().setVisibility(View.GONE);

                    final boolean deniedForever = Utils.isBluetoothScanPermissionDeniedForever(this);
                    mBinding.noBluetoothPermission.actionGrantBluetoothPermission.setVisibility(deniedForever ? View.GONE : View.VISIBLE);
                    mBinding.noBluetoothPermission.actionPermissionSettings.setVisibility(deniedForever ? View.VISIBLE : View.GONE);
                }
            } else {
                mBinding.bluetoothOff.getRoot().setVisibility(View.VISIBLE);
                mBinding.progressBar.setVisibility(View.INVISIBLE);
                mBinding.noDevices.getRoot().setVisibility(View.GONE);
                mBinding.noBluetoothPermission.getRoot().setVisibility(View.GONE);
            }
        } else {
            mBinding.noLocationPermission.getRoot().setVisibility(View.VISIBLE);
            mBinding.noBluetoothPermission.getRoot().setVisibility(View.GONE);
            mBinding.bluetoothOff.getRoot().setVisibility(View.GONE);
            mBinding.progressBar.setVisibility(View.INVISIBLE);
            mBinding.noDevices.getRoot().setVisibility(View.GONE);

            final boolean deniedForever = Utils.isLocationPermissionDeniedForever(this);
            mBinding.noLocationPermission.actionGrantLocationPermission.setVisibility(deniedForever ? View.GONE : View.VISIBLE);
            mBinding.noLocationPermission.actionPermissionSettings.setVisibility(deniedForever ? View.VISIBLE : View.GONE);
        }
    }

    private void openPermissionSettings() {
        final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void openLocationSettings() {
        final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    private void requestBluetoothEnabled() {
        final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivity(enableIntent);
    }
}
