/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import javax.inject.Inject;

import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.sample.application.Dagger2Application;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.fragment.DeviceFragment;
import io.runtime.mcumgr.sample.fragment.FilesFragment;
import io.runtime.mcumgr.sample.fragment.ImageFragment;
import io.runtime.mcumgr.sample.fragment.LogsStatsFragment;

@SuppressWarnings("ConstantConditions")
public class MainActivity extends AppCompatActivity
		implements Injectable, HasSupportFragmentInjector {
	public static final String EXTRA_DEVICE = "device";

	@Inject
	DispatchingAndroidInjector<Fragment> mDispatchingAndroidInjector;
	@Inject
	McuMgrTransport mMcuMgrTransport;

	private Fragment mDeviceFragment;
	private Fragment mImageFragment;
	private Fragment mFilesFragment;
	private Fragment mLogsStatsFragment;

	@Override
	public DispatchingAndroidInjector<Fragment> supportFragmentInjector() {
		return mDispatchingAndroidInjector;
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		// The target must be set before calling super.onCreate(Bundle).
		// Otherwise, Dagger2 will fail to inflate this Activity.
		final BluetoothDevice device = getIntent().getParcelableExtra(EXTRA_DEVICE);
		final String deviceName = device.getName();
		final String deviceAddress = device.getAddress();
		((Dagger2Application)getApplication()).setTarget(device);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Configure the view.
		final Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle(deviceName);
		getSupportActionBar().setSubtitle(deviceAddress);

		final BottomNavigationView navMenu = findViewById(R.id.nav_menu);
		navMenu.setSelectedItemId(R.id.nav_default);
		navMenu.setOnNavigationItemSelectedListener(item -> {
			switch (item.getItemId()) {
				case R.id.nav_default:
					getSupportFragmentManager().beginTransaction()
							.show(mDeviceFragment).hide(mImageFragment)
							.hide(mFilesFragment).hide(mLogsStatsFragment)
							.commit();
					return true;
				case R.id.nav_dfu:
					getSupportFragmentManager().beginTransaction()
							.hide(mDeviceFragment).show(mImageFragment)
							.hide(mFilesFragment).hide(mLogsStatsFragment)
							.commit();
					return true;
				case R.id.nav_fs:
					getSupportFragmentManager().beginTransaction()
							.hide(mDeviceFragment).hide(mImageFragment)
							.show(mFilesFragment).hide(mLogsStatsFragment)
							.commit();
					return true;
				case R.id.nav_stats:
					getSupportFragmentManager().beginTransaction()
							.hide(mDeviceFragment).hide(mImageFragment)
							.hide(mFilesFragment).show(mLogsStatsFragment)
							.commit();
					return true;
			}
			return false;
		});

		// Initialize fragments.
		if (savedInstanceState == null) {
			mDeviceFragment = new DeviceFragment();
			mImageFragment = new ImageFragment();
			mFilesFragment = new FilesFragment();
			mLogsStatsFragment = new LogsStatsFragment();

			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, mDeviceFragment, "device")
					.add(R.id.container, mImageFragment, "image")
					.add(R.id.container, mFilesFragment, "fs")
					.add(R.id.container, mLogsStatsFragment, "logs")
					// Initially, show the Device fragment and hide others.
					.hide(mImageFragment).hide(mFilesFragment).hide(mLogsStatsFragment)
					.commit();
		} else {
			mDeviceFragment = getSupportFragmentManager().findFragmentByTag("device");
			mImageFragment = getSupportFragmentManager().findFragmentByTag("image");
			mFilesFragment = getSupportFragmentManager().findFragmentByTag("fs");
			mLogsStatsFragment = getSupportFragmentManager().findFragmentByTag("logs");
		}

		// Connect the transporter
		mMcuMgrTransport.connect(null);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (isFinishing()) {
			mMcuMgrTransport.release();
			mMcuMgrTransport = null;
		}
	}
}
