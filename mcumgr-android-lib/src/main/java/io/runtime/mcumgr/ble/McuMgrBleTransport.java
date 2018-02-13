/***************************************************************************
 * Copyright (c) Intellinium SAS, 2014-present
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Intellinium SAS and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Intellinium SAS
 * and its suppliers and may be covered by French and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Intellinium SAS.
 ***************************************************************************/

package io.runtime.mcumgr.ble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.ConditionVariable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import java.util.UUID;
import java.util.concurrent.TimeoutException;

import io.runtime.mcumgr.McuManager;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrResponse;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.McuMgrException;

/* TODO */
@SuppressLint("MissingPermission") /* To get rid of android studio warnings */
public class McuMgrBleTransport extends McuMgrTransport {

	/**
	 * The current context of the Application. This context must be valid during all the
	 * FOTA operations.
	 */
	private final Context mContext;

	/**
	 * The BLE device to connect to
	 */
	private BluetoothDevice mTarget;

	/**
	 * The BLE adapter
	 */
	private BluetoothAdapter mBluetoothAdapter;

	/**
	 * The BLE gatt server of the remote device
	 */
	private BluetoothGatt mBluetoothGatt;

	/**
	 * Flag describing of the send operation is synchronous of asynchronous
	 */
	private boolean mAsync;

	/**
	 * This object will receive all the BLE related callbacks
	 */
	private final BleGattCallback bleGattCallback;

	/**
	 * Defines a blocking state of a BLe operation
	 */
	private BleSyncStep mBleSyncStep;

	private static final UUID SMP_SERVICE_UUID = UUID.fromString("8D53DC1D-1DB7-4CD3-868B-8A527460AA84");
	private static final UUID SMP_CHARAC_UUID = UUID.fromString("DA2E7828-FBCE-4E01-AE9E-261174997C48");

	/**
	 * The remote SMP service
	 */
	private BluetoothGattService mSmpService;

	/**
	 * The remote SMP characteristic
	 */
	private BluetoothGattCharacteristic mSmpCharacteristic;

	/**
	 * The constructor of this transport may throw RuntimeException depending of the permissions
	 * set in the manifest and the hardware capability.
	 *
	 * @param context A valid context
	 * @param target  The BLE device to connect to
	 */
	public McuMgrBleTransport(@NonNull Context context, @NonNull BluetoothDevice target) {
		super(McuManager.Scheme.BLE);
		this.mContext = context;
		this.mTarget = target;
		this.bleGattCallback = new BleGattCallback();

		setUpBle();
		checkPermissions();
		checkBleCapability();
	}

	/**
	 * The constructor of this transport may throw RuntimeException depending of the permissions
	 * set in the manifest and the hardware capability.
	 *
	 * @param context A valid context
	 * @param gatt  The remote gatt server
	 */
	public McuMgrBleTransport(@NonNull Context context, @NonNull BluetoothGatt gatt) {
		super(McuManager.Scheme.BLE);
		this.mContext = context;
		this.mBluetoothGatt = gatt;
		this.bleGattCallback = new BleGattCallback();

		setUpBle();
		checkPermissions();
		checkBleCapability();
	}

	/**
	 * Sets up the BLE by getting the {@link BluetoothManager} and the {@link BluetoothAdapter}.
	 *
	 * @throws IllegalStateException Either the manager or the adapter is null. This should not
	 *                               happen
	 */
	private void setUpBle() {
		final BluetoothManager bluetoothManager =
				(BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);

		if (bluetoothManager == null) {
			throw new IllegalStateException("The BluetoothManager is null");
		}

		this.mBluetoothAdapter = bluetoothManager.getAdapter();
		if (this.mBluetoothAdapter == null) {
			throw new IllegalStateException("The BluetoothAdapter is null");
		}
	}

	/**
	 * Check that the hardware supports BLE. If not, this DFU transport is not usable
	 *
	 * @throws IllegalStateException The hardware does not support BLE
	 */
	private void checkBleCapability() {
		if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			throw new IllegalStateException("The device has no BLE capability");
		}
	}

	/**
	 * Check that the application has all the permissions to use the BLE capabilities
	 * of the the phone.
	 *
	 * @throws SecurityException A permission is missing
	 */
	private void checkPermissions() {
		if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH) !=
				PackageManager.PERMISSION_GRANTED) {
			throw new SecurityException("The application doesn't have the BLUETOOTH permission");
		}

		if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_ADMIN) !=
				PackageManager.PERMISSION_GRANTED) {
			throw new SecurityException("The application doesn't have the BLUETOOTH_ADMIN permission");
		}
	}

	/**
	 * This class will receives all the BLE related callbacks. The {@link McuMgrBleTransport} can't directly extends
	 * {@link BluetoothGattCallback} as it is an abstract class and not an interface, and we already extends
	 * {@link McuMgrTransport}.
	 */
	private class BleGattCallback extends BluetoothGattCallback {

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			if (!mAsync) {
				mBleSyncStep.setResult(status, newState);
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (!mAsync) {
				mBleSyncStep.setResult(status, null);
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			if (!mAsync) {
				mBleSyncStep.setResult(0, characteristic);
			}
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			if (!mAsync) {
				mBleSyncStep.setResult(status, descriptor);
			}
		}
	}

	/**
	 * Check if the BLE is enabled. We can't enable the BLE directly in this function,
	 * as we need the result of {@link android.app.Activity#startActivityForResult(Intent, int)}.
	 * Only the application's activity can overrides the
	 * {@link android.app.Activity#onActivityResult(int, int, Intent)} function.
	 * <p>
	 * So, we just throw an error, ask the main activity to do the job, and call us again when
	 * the BLE is enabled.
	 *
	 * @throws IllegalStateException The BLE is not enabled
	 */
	private void checkBleIsEnabled() {
		if (!mBluetoothAdapter.isEnabled()) {
			throw new IllegalStateException("The BLE is not enabled");
		}
	}

	private void connectToGatt() throws TimeoutException, McuMgrException {
		if (mBluetoothGatt != null) {
			return;
		}

		mBluetoothGatt = mTarget.connectGatt(mContext, true, bleGattCallback);

		BleSyncStep.Result<Integer> r = this.mBleSyncStep.waitForResult(Integer.class);
		if (r.getStatus() != BluetoothGatt.GATT_SUCCESS ||
				r.getResult() != BluetoothGatt.STATE_CONNECTED) {
			throw new McuMgrException("Couldn't connect to the remote GATT server");
		}
	}

	private void loadServicesAndCheck() throws TimeoutException, McuMgrException {
		mBluetoothGatt.discoverServices();

		BleSyncStep.Result<Void> r = this.mBleSyncStep.waitForResult(Void.class);
		if (r.getStatus() != BluetoothGatt.GATT_SUCCESS) {
			throw new McuMgrException("Could not discover the GATT server's services");
		}

		this.mSmpService = mBluetoothGatt.getService(SMP_SERVICE_UUID);
		if (this.mSmpService == null) {
			throw new McuMgrException("The service " + SMP_SERVICE_UUID.toString() + " does not exists in the " +
					"remote gatt server");
		}

		this.mSmpCharacteristic = mSmpService.getCharacteristic(SMP_CHARAC_UUID);
		if (this.mSmpCharacteristic == null) {
			throw new McuMgrException("The characteristic " + SMP_CHARAC_UUID.toString() + " does not exists in the " +
					"remote gatt server");
		}
	}

	private void observeSmpNotifications() throws TimeoutException, McuMgrException {
		this.mBluetoothGatt.setCharacteristicNotification(this.mSmpCharacteristic, true);
		this.mBleSyncStep.waitForResult(BluetoothGattCharacteristic.class);
		/*
		 * The {@link BluetoothGattCallback#onCharacteristicChanged} doesn't return a status.
		 * so no error check here.
		 */

		UUID uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
		BluetoothGattDescriptor descriptor = this.mSmpCharacteristic.getDescriptor(uuid);
		descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		this.mBluetoothGatt.writeDescriptor(descriptor);
		BleSyncStep.Result<BluetoothGattDescriptor> r =
				this.mBleSyncStep.waitForResult(BluetoothGattDescriptor.class);
		if (r.getStatus() != BluetoothGatt.GATT_SUCCESS) {
			throw new McuMgrException("Could not discover the GATT server's services");
		}
	}

	private void sendData(byte[] data) {
		this.mSmpCharacteristic.setValue(data);
		this.mSmpCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
		this.mBluetoothGatt.writeCharacteristic(this.mSmpCharacteristic);
	}

	/* TODO: what is the format of the responses */
	private void readSmpResponse() throws TimeoutException {
		this.mBleSyncStep.waitForResult(Void.class, 20 * 1000);
	}

	/**
	 * Represents a blocking BLE operation
	 */
	private class BleSyncStep {
		private class Result<T> {
			/**
			 * The status returned by the step. Default to no error, as some BLE callbacks
			 * don't return a status.
			 */
			private int mStatus = 0;

			/**
			 * The result returned by the step
			 */
			private T mResult;

			Result(int mStatus, T mResult) {
				this.mStatus = mStatus;
				this.mResult = mResult;
			}

			int getStatus() {
				return mStatus;
			}

			void setStatus(int mStatus) {
				this.mStatus = mStatus;
			}

			T getResult() {
				return mResult;
			}

			void setResult(T mResult) {
				this.mResult = mResult;
			}
		}

		/**
		 * This condition variable is used to check is the step is done
		 */
		private final ConditionVariable mCond;

		/**
		 * The default value of the {@link ConditionVariable#block(long)} timeout.
		 */
		private final static int BLE_OP_TIMEOUT = 10000;

		private int mRawStatus;

		private Object mRawResult;

		public BleSyncStep() {
			this.mCond = new ConditionVariable();
		}

		public BleSyncStep(ConditionVariable mCond) {
			this.mCond = mCond;
		}

		synchronized <T> Result<T> waitForResult(Class<T> clz) throws TimeoutException {
			return waitForResult(clz, BLE_OP_TIMEOUT);
		}

		synchronized <T> Result<T> waitForResult(Class<T> clz, int timeout) throws TimeoutException {
			if (mCond.block(timeout)) {
				throw new TimeoutException();
			}
			if (!clz.isAssignableFrom(mRawResult.getClass())) {
				throw new ClassCastException(mRawResult.getClass() + " can't be casted to " + clz.getName());
			}

			return new Result<>(mRawStatus, (T) mRawResult);
		}

		synchronized void setResult(int status, Object result) {
			this.mRawStatus = status;
			this.mRawResult = result;
			this.mCond.open();
		}
	}

	@Override
	public McuMgrResponse send(byte[] payload) throws McuMgrException {
		this.mAsync = false;
		this.mBleSyncStep = new BleSyncStep();

		try {
			checkBleIsEnabled();

			/* 1- Connect to GATT server */
			connectToGatt();

			/* 2- Check if the SMP service exists */
			loadServicesAndCheck();

			/* 3- Observe the notifications */
			observeSmpNotifications();

			/* 4- All set up. Sending data now */
			sendData(payload);

			/* 5- Wait for the notification */
			readSmpResponse();
		} catch (McuMgrException e) {
			throw e;
		} catch (Exception e) {
			throw new McuMgrException(e);
		} finally {
			mBluetoothGatt.disconnect();
			mBluetoothGatt.close();
		}

		/* TODO: what is a McuMgrResponse? */
		return null;
	}

	@Override
	public void send(byte[] payload, McuMgrCallback callback) {

	}
}
