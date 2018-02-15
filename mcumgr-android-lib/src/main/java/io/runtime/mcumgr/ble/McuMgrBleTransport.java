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
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.ConditionVariable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrOpenCallback;
import io.runtime.mcumgr.McuMgrMtuCallback;
import io.runtime.mcumgr.McuMgrMtuProvider;
import io.runtime.mcumgr.McuMgrScheme;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.resp.McuMgrResponse;
import io.runtime.mcumgr.resp.McuMgrResponseBuilder;
import io.runtime.mcumgr.util.ByteUtil;

/* TODO */
@SuppressLint("MissingPermission") /* To get rid of android studio warnings */
public class McuMgrBleTransport extends McuMgrTransport implements McuMgrMtuProvider {

    private static final UUID SMP_SERVICE_UUID = UUID.fromString("8D53DC1D-1DB7-4CD3-868B-8A527460AA84");
    private static final UUID SMP_CHARAC_UUID = UUID.fromString("DA2E7828-FBCE-4E01-AE9E-261174997C48");
    private static final String TAG = McuMgrBleTransport.class.getSimpleName();
    /**
     * The current context of the Application. This context must be valid during all the
     * FOTA operations.
     */
    private final Context mContext;

    /**
     * true if the constructor had a {@link BluetoothGatt} as a paramater. In that case,
     * we don't automatically close the connection with the server, as the rest of the
     * application may need it
     */
    private boolean mWasAlreadyConnected = false;

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

    /**
     * The remote SMP service
     */
    private BluetoothGattService mSmpService;

    /**
     * The remote SMP characteristic
     */
    private BluetoothGattCharacteristic mSmpCharacteristic;

    /**
     * The callback used in asynchronous {@link #send(byte[], Class, McuMgrCallback)}
     */
    private McuMgrCallback mCallback;

    /**
     * The bluetooth manager
     */
    private BluetoothManager mBluetoothManager;

    /**
     * Initialization callback
     */
    private McuMgrOpenCallback mInitCb;

    /**
     * Mtu fetching callback
     */
    private McuMgrMtuCallback mMtuCb;

    /**
     * The {@link #send(byte[], Class)} response type
     */
    private Class<? extends McuMgrResponse> mResponseType;

    /**
     * The negociated MTU
     */
    private int mMtu;

    /**
     * Byte output stream to reassemble notification fragments
     */
    private ByteArrayOutputStream mByteOutput;

    /**
     * The constructor of this transport may throw RuntimeException depending of the permissions
     * set in the manifest and the hardware capability.
     *
     * @param context A valid context
     * @param target  The BLE device to connect to
     */
    public McuMgrBleTransport(@NonNull Context context, @NonNull BluetoothDevice target) {
        super(McuMgrScheme.BLE);
        this.mContext = context;
        this.mTarget = target;
        this.bleGattCallback = new BleGattCallback();
        this.mByteOutput = new ByteArrayOutputStream();

        setUpBle();
        checkPermissions();
        checkBleCapability();
    }

    /**
     * The constructor of this transport may throw RuntimeException depending of the permissions
     * set in the manifest and the hardware capability.
     *
     * @param context A valid context
     * @param gatt    The remote gatt server
     */
    public McuMgrBleTransport(@NonNull Context context, @NonNull BluetoothGatt gatt) {
        super(McuMgrScheme.BLE);
        this.mContext = context;
        this.mBluetoothGatt = gatt;
        this.mWasAlreadyConnected = true;
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
        mBluetoothManager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);

        if (this.mBluetoothManager == null) {
            throw new IllegalStateException("The BluetoothManager is null");
        }

        this.mBluetoothAdapter = mBluetoothManager.getAdapter();
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
            Log.d(TAG, "GATT connection state changed: status " + status + ", state " + newState);

            if (newState == BluetoothGatt.STATE_DISCONNECTED ||
                    newState == BluetoothGatt.STATE_DISCONNECTING) {
                return;
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Discovering services");
                mBluetoothGatt.discoverServices();
            } else {
                mInitCb.onInitError();
                if (!mWasAlreadyConnected) {
                    mBluetoothGatt.disconnect();
                    mBluetoothGatt.close();
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "Services discovered, status " + status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                try {
                    initSmpServiceAndCharac();
                    Log.d(TAG, "Enabling notifications");
                    mBluetoothGatt.setCharacteristicNotification(mSmpCharacteristic, true);
                    UUID uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                    BluetoothGattDescriptor descriptor = mSmpCharacteristic.getDescriptor(uuid);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothGatt.writeDescriptor(descriptor);
                } catch (McuMgrException e) {
                    e.printStackTrace();
                    mInitCb.onInitError();
                    if (!mWasAlreadyConnected) {
                        mBluetoothGatt.disconnect();
                        mBluetoothGatt.close();
                    }
                }
            } else {
                mInitCb.onInitError();
                if (!mWasAlreadyConnected) {
                    mBluetoothGatt.disconnect();
                    mBluetoothGatt.close();
                }
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "Response received from remote GATT service");
            Log.d(TAG, ByteUtil.byteArrayToHex(characteristic.getValue(), "0x%02X "));

            byte[] res = characteristic.getValue();
            try {
                mByteOutput.write(res);
            } catch (IOException e) {
                mCallback.onError(new McuMgrException(e));
                if (!mWasAlreadyConnected) {
                    mBluetoothGatt.disconnect();
                    mBluetoothGatt.close();
                }
            }

            if (res[res.length - 1] == -1) {
                if (!mAsync) {
                    mBleSyncStep.setResult(0, null);
                    mByteOutput = new ByteArrayOutputStream();
                } else {
                    try {
                        McuMgrResponseBuilder builder =
                                new McuMgrResponseBuilder(McuMgrScheme.BLE, mByteOutput.toByteArray());
                        mByteOutput = new ByteArrayOutputStream();
                        mCallback.onResponse(builder.build(mResponseType));
                    } catch (IOException e) {
                        mCallback.onError(new McuMgrException(e));
                        if (!mWasAlreadyConnected) {
                            mBluetoothGatt.disconnect();
                            mBluetoothGatt.close();
                        }
                    }
                }
            } else {
                Log.d(TAG, "Need more data, as the last byte is " + res[res.length - 1]);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "Descriptor written: status " + status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                mInitCb.onInitSuccess();
            } else {
                mInitCb.onInitError();
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            mMtu = mtu;
            if (mMtuCb != null) {
                Log.d(TAG, "MTU fetched: " + mtu);
                mMtuCb.onMtuFetched(mtu);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if(!mAsync) {
                return;
            }

            if (status != BluetoothGatt.GATT_SUCCESS) {
                mCallback.onError(new McuMgrException("Couldn't write in characteristic, status " + status));
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
        Log.d(TAG, "Checking if BLE is ON");

        if (!mBluetoothAdapter.isEnabled()) {
            throw new IllegalStateException("The BLE is not enabled");
        }
    }

    private void connectToGatt() {
        if (mBluetoothGatt != null) {
            return;
        }

        Log.d(TAG, "Connecting to GATT server");
        mBluetoothGatt = mTarget.connectGatt(mContext, true, bleGattCallback);
    }

    private void initSmpServiceAndCharac() throws McuMgrException {
        Log.d(TAG, "Checking if the SMP service is served by the server");
        this.mSmpService = mBluetoothGatt.getService(SMP_SERVICE_UUID);
        if (this.mSmpService == null) {
            throw new McuMgrException("The service " + SMP_SERVICE_UUID.toString() + " does not exists in the " +
                    "remote gatt server");
        }

        Log.d(TAG, "Checking if the SMP characteristic is served by the server");
        this.mSmpCharacteristic = mSmpService.getCharacteristic(SMP_CHARAC_UUID);
        if (this.mSmpCharacteristic == null) {
            throw new McuMgrException("The characteristic " + SMP_CHARAC_UUID.toString() + " does not exists in the " +
                    "remote gatt server");
        }
    }

    private void sendData(byte[] data) {
        this.mSmpCharacteristic.setValue(data);
        this.mSmpCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        Log.d(TAG, "Sending " + ByteUtil.byteArrayToHex(data, "0x%02X "));
        this.mBluetoothGatt.writeCharacteristic(this.mSmpCharacteristic);
    }

    private <T extends McuMgrResponse> T readSmpResponse(Class<T> respType)
            throws TimeoutException, McuMgrException, IOException {
        if (this.mAsync) {
            return null;
        }

        BleSyncStep.Result<Void> r = this.mBleSyncStep.waitForResult(Void.class, 20 * 1000);
        if (r.getStatus() != BluetoothGatt.GATT_SUCCESS) {
            throw new McuMgrException("Could not get the mcumgr response");
        }

        McuMgrResponseBuilder builder = new McuMgrResponseBuilder(McuMgrScheme.BLE, mByteOutput.toByteArray());
        return builder.build(respType);
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
            private int mStatus = BluetoothGatt.GATT_SUCCESS;

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

            T getResult() {
                return mResult;
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

        BleSyncStep() {
            this.mCond = new ConditionVariable();
        }

        synchronized <T> Result<T> waitForResult(Class<T> clz) throws TimeoutException {
            return waitForResult(clz, BLE_OP_TIMEOUT);
        }

        @SuppressWarnings("unchecked")
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

    private boolean isAlreadySetUp() {
        boolean b = this.mBluetoothGatt != null &&
                this.mBluetoothManager.getConnectionState(this.mTarget, BluetoothProfile.GATT) ==
                        BluetoothProfile.STATE_CONNECTED;

        Log.d(TAG, "The transporter is already set up? " + b);

        return b;
    }

    @Override
    public <T extends McuMgrResponse> T send(byte[] payload, Class<T> responseType) throws McuMgrException {
        this.mAsync = false;
        this.mBleSyncStep = new BleSyncStep();

        try {
            sendData(payload);

            return readSmpResponse(responseType);
        } catch (McuMgrException e) {
            throw e;
        } catch (Exception e) {
            throw new McuMgrException(e);
        } finally {
            if (!this.mWasAlreadyConnected) {
                mBluetoothGatt.disconnect();
                mBluetoothGatt.close();
            }
        }
    }

    @Override
    public <T extends McuMgrResponse> void send(byte[] payload, Class<T> responseType, McuMgrCallback<T> callback) {
        this.mAsync = true;
        this.mCallback = callback;
        this.mResponseType = responseType;

        sendData(payload);
    }

    @Override
    public void getMtu(McuMgrMtuCallback cb) {
        this.mMtuCb = cb;

        if (!isAlreadySetUp()) {
            cb.onMtuError();
        } else {
            this.mBluetoothGatt.requestMtu(512);
        }
    }

    @Override
    public boolean initAfterReset() {
        return true;
    }

    @Override
    public void open(McuMgrOpenCallback cb) {
        this.mInitCb = cb;

        if (isAlreadySetUp()) {
            this.mInitCb.onInitSuccess();
        } else {
            checkBleIsEnabled();
            connectToGatt();
        }
    }

    @Override
    public void close() {
        mBluetoothGatt.disconnect();
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }
}
