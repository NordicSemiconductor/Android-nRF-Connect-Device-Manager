/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample;

import android.Manifest;
import android.arch.lifecycle.ViewModelProviders;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.runtime.mcumgr.sample.adapter.DevicesAdapter;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.utils.Utils;
import io.runtime.mcumgr.sample.viewmodel.ScannerStateLiveData;
import io.runtime.mcumgr.sample.viewmodel.ScannerViewModel;
import io.runtime.mcumgr.sample.viewmodel.ViewModelFactory;

public class ScannerActivity extends AppCompatActivity
		implements Injectable, DevicesAdapter.OnItemClickListener {
	private static final int REQUEST_ACCESS_COARSE_LOCATION = 1022; // random number

	@Inject
	ViewModelFactory mViewModelFactory;

	@BindView(R.id.progress_bar)
	ProgressBar mScanningProgressBar;
	@BindView(R.id.no_devices)
	View mEmptyView;
	@BindView(R.id.no_location_permission)
	View mNoLocationPermissionView;
	@BindView(R.id.action_grant_location_permission)
	Button mGrantPermissionButton;
	@BindView(R.id.action_permission_settings)
	Button mPermissionSettingsButton;
	@BindView(R.id.no_location)
	View mNoLocationView;
	@BindView(R.id.bluetooth_off)
	View mNoBluetoothView;

	private ScannerViewModel mScannerViewModel;

	@SuppressWarnings("ConstantConditions")
	@Override
	protected void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scanner);
		ButterKnife.bind(this);

		final Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setTitle(R.string.app_name);

		// Create view model containing utility methods for scanning
		mScannerViewModel = ViewModelProviders.of(this, mViewModelFactory)
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
		stopScan();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.filter, menu);
		menu.findItem(R.id.filter_uuid).setChecked(mScannerViewModel.isUuidFilterEnabled());
        menu.findItem(R.id.filter_nearby).setChecked(mScannerViewModel.isNearbyFilterEnabled());
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case R.id.filter_uuid:
				item.setChecked(!item.isChecked());
				mScannerViewModel.filterByUuid(item.isChecked());
				return true;
			case R.id.filter_nearby:
				item.setChecked(!item.isChecked());
				mScannerViewModel.filterByDistance(item.isChecked());
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onItemClick(final BluetoothDevice device) {
		final Intent intent = new Intent(this, MainActivity.class);
		intent.putExtra(MainActivity.EXTRA_DEVICE, device);
		startActivity(intent);
	}

	@Override
	public void onRequestPermissionsResult(final int requestCode,
										   @NonNull final String[] permissions,
										   @NonNull final int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		switch (requestCode) {
			case REQUEST_ACCESS_COARSE_LOCATION:
				mScannerViewModel.refresh();
				break;
		}
	}

	@OnClick(R.id.action_enable_location)
	public void onEnableLocationClicked() {
		final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivity(intent);
	}

	@OnClick(R.id.action_enable_bluetooth)
	public void onEnableBluetoothClicked() {
		final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivity(enableIntent);
	}

	@OnClick(R.id.action_grant_location_permission)
	public void onGrantLocationPermissionClicked() {
		Utils.markLocationPermissionRequested(this);
		ActivityCompat.requestPermissions(this,
				new String[] { Manifest.permission.ACCESS_COARSE_LOCATION },
				REQUEST_ACCESS_COARSE_LOCATION
		);
	}

	@OnClick(R.id.action_permission_settings)
	public void onPermissionSettingsClicked() {
		final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
		intent.setData(Uri.fromParts("package", getPackageName(), null));
		startActivity(intent);
	}

	/**
	 * Start scanning for Bluetooth devices or displays a message based on the scanner state.
	 */
	private void startScan(final ScannerStateLiveData state) {
		// First, check the Location permission.
		// This is required on Marshmallow onwards in order to scan for Bluetooth LE devices.
		if (Utils.isLocationPermissionsGranted(this)) {
			mNoLocationPermissionView.setVisibility(View.GONE);

			// Bluetooth must be enabled
			if (state.isBluetoothEnabled()) {
				mNoBluetoothView.setVisibility(View.GONE);

				// We are now OK to start scanning
				mScannerViewModel.startScan();
				mScanningProgressBar.setVisibility(View.VISIBLE);

				if (!state.hasRecords()) {
					mEmptyView.setVisibility(View.VISIBLE);

					if (!Utils.isLocationRequired(this) ||
						Utils.isLocationEnabled(this)) {
						mNoLocationView.setVisibility(View.INVISIBLE);
					} else {
						mNoLocationView.setVisibility(View.VISIBLE);
					}
				} else {
					mEmptyView.setVisibility(View.GONE);
				}
			} else {
				mNoBluetoothView.setVisibility(View.VISIBLE);
				mScanningProgressBar.setVisibility(View.INVISIBLE);
				mEmptyView.setVisibility(View.GONE);
			}
		} else {
			mNoLocationPermissionView.setVisibility(View.VISIBLE);
			mNoBluetoothView.setVisibility(View.GONE);
			mScanningProgressBar.setVisibility(View.INVISIBLE);
			mEmptyView.setVisibility(View.GONE);

			final boolean deniedForever = Utils.isLocationPermissionDeniedForever(this);
			mGrantPermissionButton.setVisibility(deniedForever ? View.GONE : View.VISIBLE);
			mPermissionSettingsButton.setVisibility(deniedForever ? View.VISIBLE : View.GONE);
		}
	}

	/**
	 * stop scanning for bluetooth devices.
	 */
	private void stopScan() {
		mScannerViewModel.stopScan();
	}
}
