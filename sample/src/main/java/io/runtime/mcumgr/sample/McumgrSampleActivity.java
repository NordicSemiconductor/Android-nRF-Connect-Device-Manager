package io.runtime.mcumgr.sample;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import io.runtime.mcumgr.ble.McuMgrBleTransport;
import io.runtime.mcumgr.dfu.FirmwareUpgradeCallback;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;
import io.runtime.mcumgr.exception.McuMgrException;

public class McumgrSampleActivity extends AppCompatActivity
		implements BluetoothAdapter.LeScanCallback, FirmwareUpgradeCallback {

	private LinearLayout mLoading;
	private BluetoothAdapter mBluetoothAdapter;
	private Handler mHandler;
	private String mDeviceName;
	private BluetoothDevice mDevice;
	private Uri mPath;
	private AlertDialog mProgressDialog;
	private ProgressBar mProgressBar;
	private FloatingActionButton mFab;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.mHandler = new Handler();
		setContentView(R.layout.activity_main);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		mFab = findViewById(R.id.start_fota);
		mFab.setOnClickListener(v -> chooseFile());

		mLoading = findViewById(R.id.loading);

		ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
						Manifest.permission.ACCESS_FINE_LOCATION}, 2);
	}

	private void showStartFota() {
		final EditText name = new EditText(this);

		TypedValue typedValue = new TypedValue();
		getTheme().resolveAttribute(R.attr.dialogPreferredPadding, typedValue, true);

		final DisplayMetrics metrics = new android.util.DisplayMetrics();
		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		wm.getDefaultDisplay().getMetrics(metrics);
		int pad = (int) typedValue.getDimension(metrics);
		name.setPadding(pad, pad, pad, pad);

		new AlertDialog.Builder(this)
				.setTitle("Starting FOTA")
				.setMessage("What's the name of the BLE device to flash?")
				.setView(name)
				.setPositiveButton("Start", (dialog, which) -> {
					dialog.dismiss();
					mDeviceName = name.getText().toString();
					findDevice();
				})
				.setNegativeButton("Cancel", null)
				.show();
	}

	private void findDevice() {
		final BluetoothManager bluetoothManager =
				(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		if (bluetoothManager != null) {
			mBluetoothAdapter = bluetoothManager.getAdapter();
		} else {
			return;
		}

		if (!mBluetoothAdapter.isEnabled()) {
			new AlertDialog.Builder(this)
					.setTitle("Bluetooth error")
					.setMessage("Please turn on the bluetooth")
					.setPositiveButton("OK", null)
					.show();
			return;
		}

		scanLeDevice(true);
	}

	private void chooseFile() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*");
		startActivityForResult(intent, 1);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1 && resultCode == RESULT_OK) {
			mPath = data.getData();
			showStartFota();
		}

		if (requestCode == 2 && resultCode != RESULT_OK) {
			finish();
		}
	}

	private Runnable mScanRunnable = () -> {
		scanLeDevice(false);
		new AlertDialog.Builder(McumgrSampleActivity.this)
				.setTitle("Bluetooth error")
				.setMessage("Could not find the device. Ensure that it is turned on and has its " +
						"bluetooth enable")
				.setPositiveButton("OK", null)
				.show();
	};

	private void scanLeDevice(final boolean enable) {
		if (enable) {
			mLoading.setVisibility(View.VISIBLE);
			mFab.setEnabled(false);
			mHandler.postDelayed(mScanRunnable, 10 * 1000);
			mBluetoothAdapter.startLeScan(this);
		} else {
			mHandler.removeCallbacks(mScanRunnable);
			mLoading.setVisibility(View.INVISIBLE);
			mFab.setEnabled(true);
			mBluetoothAdapter.stopLeScan(this);
		}
	}

	@Override
	public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
		String name = device.getName();
		if (name == null) {
			return;
		}

		if (name.equals(mDeviceName)) {
			mDevice = device;
			scanLeDevice(false);
			prepareFota();
		}
	}

	private byte[] getBytesFromFile() throws IOException {
		InputStream inputStream = getContentResolver().openInputStream(mPath);
		if (inputStream == null) {
			throw new IOException();
		}

		ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
		int bufferSize = 1024;
		byte[] buffer = new byte[bufferSize];

		int len;
		while ((len = inputStream.read(buffer)) != -1) {
			byteBuffer.write(buffer, 0, len);
		}
		return byteBuffer.toByteArray();
	}

	private void prepareFota() {
		byte[] data;
		try {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
				Path path = Paths.get(mPath.getPath());
				data = Files.readAllBytes(path);
			} else {
				data = getBytesFromFile();
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		McuMgrBleTransport mBleTransport = new McuMgrBleTransport(this, mDevice);
		FirmwareUpgradeManager mManager = new FirmwareUpgradeManager(mBleTransport, data, this);

		mManager.start();
	}

	@Override
	public boolean confirmUpgrade(FirmwareUpgradeManager firmwareUpgrade) {
		return true;
	}

	@Override
	public void onStateChanged(FirmwareUpgradeManager.State prevState, FirmwareUpgradeManager.State newState) {

	}

	@Override
	public void onSuccess() {
		new AlertDialog.Builder(this)
				.setTitle("FOTA completed")
				.setMessage("Success !")
				.setPositiveButton("OK", null)
				.show();
	}

	@Override
	public void onFail(FirmwareUpgradeManager.State state, McuMgrException error) {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
		}

		new AlertDialog.Builder(this)
				.setTitle("FOTA error")
				.setMessage(error.getMessage())
				.setPositiveButton("OK", null)
				.show();

		error.printStackTrace();
	}

	@Override
	public void onCancel(FirmwareUpgradeManager.State state) {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
		}
	}

	@Override
	public void onUploadProgressChanged(int bytesSent, int imageSize, Date ts) {
		if (mProgressDialog == null) {
			mProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
			mProgressDialog = new AlertDialog.Builder(this)
					.setMessage("FOTA progress")
					.setView(mProgressBar)
					.create();
		}
		mProgressDialog.show();
		mProgressBar.setProgress(bytesSent * 100 / imageSize);
	}
}
