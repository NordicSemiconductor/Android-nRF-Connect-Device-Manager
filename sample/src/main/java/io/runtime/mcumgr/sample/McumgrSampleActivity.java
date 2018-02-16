/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.runtime.mcumgr.sample;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Locale;

import io.runtime.mcumgr.McuMgrOpenCallback;
import io.runtime.mcumgr.ble.McuMgrBleTransport;
import io.runtime.mcumgr.cfg.McuMgrConfig;
import io.runtime.mcumgr.dfu.FirmwareUpgradeCallback;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;
import io.runtime.mcumgr.exception.McuMgrException;

public class McumgrSampleActivity extends AppCompatActivity
        implements BluetoothAdapter.LeScanCallback, FirmwareUpgradeCallback, McuMgrOpenCallback {

    private static final String DEFAULT_DEVICE_NAME = "Zephyr";

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private String mDeviceName;
    private BluetoothDevice mDevice;
    private Uri mPath;
    private FloatingActionButton mFab;
    private TextView mBle, mResult, mProgress, mState, mFail, mSize, mFile;
    private FirmwareUpgradeManager mManager;
    private McuMgrBleTransport mBleTransport;
    private CheckBox mCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mHandler = new Handler();
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mFab = findViewById(R.id.start_fota);
        mFab.setOnClickListener(v -> chooseFile());

        mBle = findViewById(R.id.ble);
        mResult = findViewById(R.id.result);
        mProgress = findViewById(R.id.progress);
        mState = findViewById(R.id.state);
        mFail = findViewById(R.id.fail);
        mSize = findViewById(R.id.size);
        mFile = findViewById(R.id.file);
        mCheck = findViewById(R.id.legacy);

        mCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                McuMgrConfig.legacy();
            } else {
                McuMgrConfig.defaultConfig();
            }
        });
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, 2);
    }

    private void showStartFota() {
        final EditText name = new EditText(this);
        name.setText(DEFAULT_DEVICE_NAME);
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
                    mFab.hide();
                    mBle.setText("Searching");
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
            mFab.show();
            clearTextFields();
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
            clearTextFields();
            showFileName();
            showStartFota();
        }

        if (requestCode == 2 && resultCode != RESULT_OK) {
            finish();
        }
    }

    private void clearTextFields() {
        mBle.setText("");
        mResult.setText("");
        mProgress.setText("");
        mState.setText("");
        mFail.setText("");
        mSize.setText("");
        mFile.setText("");
    }

    private void showFileName() {
        String result = null;
        if (mPath.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(mPath, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = mPath.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }

        mFile.setText(result);
    }

    private Runnable mScanRunnable = () -> {
        scanLeDevice(false);
        new AlertDialog.Builder(McumgrSampleActivity.this)
                .setTitle("Bluetooth error")
                .setMessage("Could not find the device. Ensure that it is turned on and has its " +
                        "bluetooth enable")
                .setPositiveButton("OK", null)
                .show();
        mFab.show();
    };

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(mScanRunnable, 10 * 1000);
            mBluetoothAdapter.startLeScan(this);
        } else {
            mHandler.removeCallbacks(mScanRunnable);
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
            try {
                prepareFota();
                mBle.setText("Found");
            } catch (McuMgrException e) {
                e.printStackTrace();
            }
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

    private void prepareFota() throws McuMgrException {
        byte[] data;
        try {
            data = getBytesFromFile();
            mSize.setText(data.length / 1000 + " kB");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        mBleTransport = new McuMgrBleTransport(this, mDevice);
        mManager = new FirmwareUpgradeManager(mBleTransport, data, this);
        mManager.setCallbackOnUiThread(this);
        mManager.init(this);
    }

    @Override
    public void onOpen() {
        mManager.start();
        /*DefaultManager dftManager = new DefaultManager(mBleTransport);
		dftManager.echo("Hi!",
				new McuMgrCallback() {
					@Override
					public void onResponse(McuMgrResponse response) {
						String s = toHex(response.getPayload());
						Log.d("a", s);
						runOnUiThread(() -> mFail.setText(s));
					}

					@Override
					public void onError(McuMgrException error) {
						error.printStackTrace();
					}
				});*/
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private String toHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    public void onOpenError() {
        mResult.setText("FOTA open failed");
        new AlertDialog.Builder(this)
                .setTitle("FOTA error")
                .setPositiveButton("OK", null)
                .show();
        mFab.show();
    }

    @Override
    public boolean confirmUpgrade(FirmwareUpgradeManager firmwareUpgrade) {
        return true;
    }

    @Override
    public void onStateChanged(FirmwareUpgradeManager.State prevState, FirmwareUpgradeManager.State newState) {
        mState.setText(newState.name());
    }

    @Override
    public void onSuccess() {
        mResult.setText("Success");
        new AlertDialog.Builder(this)
                .setTitle("FOTA completed")
                .setMessage("Success !")
                .setPositiveButton("OK", null)
                .show();
        mFab.show();
    }

    @Override
    public void onFail(FirmwareUpgradeManager.State state, McuMgrException error) {
        mResult.setText("Fail");
        mFail.setText(error.getMessage());

        new AlertDialog.Builder(this)
                .setTitle("FOTA error")
                .setPositiveButton("OK", null)
                .show();

        mFab.show();
        error.printStackTrace();
    }

    @Override
    public void onCancel(FirmwareUpgradeManager.State state) {
        mResult.setText("Cancel");
        mFab.show();
    }

    @Override
    public void onUploadProgressChanged(int bytesSent, int imageSize, Date ts) {
        mProgress.setText(String.format(Locale.getDefault(), "%d%%", bytesSent * 100 / imageSize));
    }
}
