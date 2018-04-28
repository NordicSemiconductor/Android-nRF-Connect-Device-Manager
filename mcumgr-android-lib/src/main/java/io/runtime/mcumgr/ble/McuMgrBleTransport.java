package io.runtime.mcumgr.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.ConditionVariable;
import android.util.Log;

import java.util.Deque;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrScheme;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.ble.manager.BleManager;
import io.runtime.mcumgr.exception.InsufficientMtuException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.McuMgrResponse;

/**
 * The McuMgrBleTransport is an implementation for the {@link McuMgrScheme#BLE} transport scheme.
 * This class extends {@link BleManager}, which handles the BLE state machine and owns the
 * {@link BluetoothGatt} object that executes BLE actions. If you wish to integrate McuManager an
 * existing BLE implementation, you may simply implement {@link McuMgrTransport} or use this class
 * to perform your BLE actions by calling {@link BleManager#enqueue(Request)} and setting callbacks
 * using {@link McuMgrBleTransport#setGattCallbacks(McuMgrBleCallbacks)}.
 */
public class McuMgrBleTransport extends BleManager<McuMgrBleCallbacks> implements McuMgrTransport {

    private final static String TAG = "McuMgrBleTransport";

    private static final UUID SMP_SERVICE_UUID =
            UUID.fromString("8D53DC1D-1DB7-4CD3-868B-8A527460AA84");
    private static final UUID SMP_CHAR_UUID =
            UUID.fromString("DA2E7828-FBCE-4E01-AE9E-261174997C48");

    /**
     * Simple Management Protocol service
     */
    private BluetoothGattService mSmpService;

    /**
     * Simple Management Protocol characteristic.
     */
    private BluetoothGattCharacteristic mSmpCharacteristic;

    /**
     * Used to wait while a device is being connected and set up. This lock is opened once the
     * device is ready (opened in onDeviceReady).
     */
    private ConditionVariable mReadyLock = new ConditionVariable(false);

    /**
     * Queue of requests to send from Mcu Manager
     */
    private LinkedBlockingQueue<McuMgrRequest> mSendQueue = new LinkedBlockingQueue<>();

    /**
     * Used to wait while a writeCharacteristic request is sent. This lock is opened when a
     * notification is received (onCharacteristicNotified) or an error occurs (onError).
     */
    private ConditionVariable mSendLock = new ConditionVariable(false);

    /**
     * The MTU being used by this device.
     */
    private int mMtu = 23;

    /**
     * The current request being sent. Used to finish or fail the request from an asynchronous
     * mCallback.
     */
    private McuMgrRequest mRequest;

    /**
     * The bluetooth device for this transporter
     */
    private BluetoothDevice mDevice;

    /**
     * Optional external callbacks
     */
    private McuMgrBleCallbacks mExternalManagerCallbacks;

    /**
     * Construct a McuMgrBleTransport object.
     *
     * @param context The context used to connect to the device
     * @param device  the device to connect to and communicate with
     */
    public McuMgrBleTransport(Context context, BluetoothDevice device) {
        super(context);
        mDevice = device;
        super.setGattCallbacks(new CallbackForwarder());
        new SendThread().start();
    }

    @Override
    public void setGattCallbacks(McuMgrBleCallbacks callbacks) {
        mExternalManagerCallbacks = callbacks;
    }

    @Override
    protected BleManagerGattCallback getGattCallback() {
        return mGattCallback;
    }

    //*******************************************************************************************
    // Mcu Manager Transport
    //*******************************************************************************************

    @Override
    public McuMgrScheme getScheme() {
        return McuMgrScheme.BLE;
    }

    @Override
    public <T extends McuMgrResponse> T send(byte[] payload, Class<T> responseType)
            throws McuMgrException {

        return new McuMgrRequest<>(payload, responseType, null).synchronous(mSendQueue);
    }

    @Override
    public <T extends McuMgrResponse> void send(byte[] payload, Class<T> responseType,
                                                McuMgrCallback<T> callback) {
        new McuMgrRequest<>(payload, responseType, callback).asynchronous(mSendQueue);
    }

    //*******************************************************************************************
    // Mcu Manager Main Send Thread
    // TODO Look into disconnects causing race conditions
    //*******************************************************************************************

    /**
     * This thread loops through the send queue blocking until a request is available. Once a
     * request is popped, connection and setup is performed if necessary, otherwise the request is
     * performed.
     */
    private class SendThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (true) {
                try {
                    // Take a request for the queue, blocking until available.
                    mRequest = mSendQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    continue;
                }

                // If device is not connected, connect
                if (!isConnected()) {
                    // Close the ready lock before
                    mReadyLock.close();
                    connect(mDevice);
                }

                // Wait until device is ready
                if (!mReadyLock.block(25 * 1000)) {
                    // On timeout, fail the request
                    mRequest.fail(new McuMgrException("Connection routine timed out."));
                    continue;
                }

                // Ensure that device supports SMP Service
                if (mSmpCharacteristic == null) {
                    if (!isConnected()) {
                        mRequest.fail(new McuMgrException("Device has disconnected"));
                    } else {
                        mRequest.fail(new McuMgrException("Device does not support SMP Service"));
                    }
                    continue;
                }

                // Ensure the mtu is sufficient
                if (mMtu < mRequest.getBytes().length) {
                    mRequest.fail(new InsufficientMtuException(mMtu));
                    continue;
                }

                // Close the send lock
                mSendLock.close();

                // Write the characteristic
                mSmpCharacteristic.setValue(mRequest.getBytes());
                Log.d(TAG, "Writing characteristic (" + mRequest.getBytes().length + " bytes)");
                boolean enqueued = writeCharacteristic(mSmpCharacteristic);

                // If the request did not get enqueued, error the request
                if (!enqueued) {
                    mRequest.fail(new McuMgrException(
                            "Write characteristic request could not be enqueued."));
                    continue;
                }

                // Block until the response is received
                if (!mSendLock.block(10 * 1000)) {
                    mRequest.fail(new McuMgrException("Send timed out."));
                }
            }
        }
    }


    //*******************************************************************************************
    // Ble Manager Callbacks
    //*******************************************************************************************

    private final BleManagerGattCallback mGattCallback = new BleManagerGattCallback() {

        // Determines whether the device supports the SMP Service
        @Override
        protected boolean isRequiredServiceSupported(BluetoothGatt gatt) {
            mSmpService = gatt.getService(SMP_SERVICE_UUID);
            if (mSmpService == null) {
                Log.e(TAG, "Device does not support SMP service");
                return false;
            }
            mSmpCharacteristic = mSmpService.getCharacteristic(SMP_CHAR_UUID);
            if (mSmpCharacteristic == null) {
                Log.e(TAG, "Device does not support SMP characteristic");
                return false;
            } else {
                final int rxProperties = mSmpCharacteristic.getProperties();
                boolean write = (rxProperties &
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0;
                boolean notify = (rxProperties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0;
                if (!write || !notify) {
                    Log.e(TAG, "SMP characteristic does not support write(" + write +
                            ") or notify(" + notify + ")");
                    return false;
                }
            }
            return true;
        }

        // Called once the connection has been established and services discovered. This method
        // adds a queue of requests necessary to set up the SMP service to begin writing
        // commands and receiving responses. Once these actions have completed onDeviceReady is
        // called
        @Override
        protected Deque<Request> initGatt(BluetoothGatt gatt) {
            final LinkedList<Request> requests = new LinkedList<>();
            requests.push(Request.newEnableNotificationsRequest(mSmpCharacteristic));
            requests.push(Request.newMtuRequest(512));
            return requests;
        }

        // Called once the device is ready. This method opens the lock waiting for the device to
        // become ready.
        @Override
        protected void onDeviceReady() {
            Log.d(TAG, "Device is ready");
            mReadyLock.open();
        }

        // Called when the device has disconnected. This method nulls the services and
        // characteristic variables and opens any waiting locks.
        @Override
        protected void onDeviceDisconnected() {
            mSmpService = null;
            mSmpCharacteristic = null;
            mReadyLock.open();
            mSendLock.open();
        }

        // Keeps track of the MTU being used to send insufficient MTU exceptions
        @Override
        protected void onMtuChanged(int mtu) {
            mMtu = mtu;

            // Call external callback
            mCallbacks.onMtuChanged(mtu);
        }

        // Called when a characteristic gets notified. Check that the characteristic is the SMP
        // Characteristic and finish the request
        @Override
        protected void onCharacteristicNotified(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(mSmpCharacteristic.getUuid())) {
                if (mRequest != null) {
                    boolean isFinished = mRequest.receive(characteristic.getValue());
                    if (!isFinished) {
                        // Forward callback
                        mCallbacks.onCharacteristicNotified(gatt.getDevice(), characteristic);
                        return;
                    }
                }
                mSendLock.open();
            }

            // Forward callback
            mCallbacks.onCharacteristicNotified(gatt.getDevice(), characteristic);
        }

        // Forwarded callbacks. The following callbacks are only used to forward callbacks
        @Override
        protected void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicRead(gatt, characteristic);
            mCallbacks.onCharacteristicRead(gatt.getDevice(), characteristic);
        }

        @Override
        protected void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicWrite(gatt, characteristic);
            mCallbacks.onCharacteristicWrite(gatt.getDevice(), characteristic);
        }

        @Override
        protected void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor) {
            super.onDescriptorRead(gatt, descriptor);
            mCallbacks.onDescriptorRead(gatt.getDevice(), descriptor);
        }

        @Override
        protected void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor) {
            super.onDescriptorWrite(gatt, descriptor);
            mCallbacks.onDescriptorWrite(gatt.getDevice(), descriptor);
        }

        @Override
        protected void onCharacteristicIndicated(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicIndicated(gatt, characteristic);
            mCallbacks.onCharacteristicIndicated(gatt.getDevice(), characteristic);
        }
    };

    private class CallbackForwarder extends McuMgrBleCallbacks {

        // This method will error the current request.
        // TODO if user queues an independent request, the request may cause an error and fail the current McuMgr request.
        @Override
        public void onError(BluetoothDevice device, String message, int errorCode) {
            if (mRequest != null) {
                mSendLock.open();
                mRequest.fail(new McuMgrException(message));
            }

            // Forward callback
            if (mExternalManagerCallbacks != null) {
                mExternalManagerCallbacks.onError(device, message, errorCode);
            }
        }

        // Forwarded callbacks
        @Override
        public void onCharacteristicNotified(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {
            if (mExternalManagerCallbacks != null) {
                mExternalManagerCallbacks.onCharacteristicNotified(device, characteristic);
            }
        }
        @Override
        public void onCharacteristicIndicated(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {
            if (mExternalManagerCallbacks != null) {
                mExternalManagerCallbacks.onCharacteristicIndicated(device, characteristic);
            }
        }
        @Override
        public void onCharacteristicRead(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {
            if (mExternalManagerCallbacks != null) {
                mExternalManagerCallbacks.onCharacteristicRead(device, characteristic);
            }
        }
        @Override
        public void onCharacteristicWrite(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {
            if (mExternalManagerCallbacks != null) {
                mExternalManagerCallbacks.onCharacteristicWrite(device, characteristic);
            }
        }
        @Override
        public void onDescriptorRead(BluetoothDevice device, BluetoothGattDescriptor descriptor) {
            if (mExternalManagerCallbacks != null) {
                mExternalManagerCallbacks.onDescriptorRead(device, descriptor);
            }
        }
        @Override
        public void onDescriptorWrite(BluetoothDevice device, BluetoothGattDescriptor descriptor) {
            if (mExternalManagerCallbacks != null) {
                mExternalManagerCallbacks.onDescriptorWrite(device, descriptor);
            }
        }
        @Override
        public void onMtuChanged(int mtu) {
            if (mExternalManagerCallbacks != null) {
                mExternalManagerCallbacks.onMtuChanged(mtu);
            }
        }
        @Override
        public void onDeviceConnecting(BluetoothDevice device) {
            if (mExternalManagerCallbacks != null) {
                mExternalManagerCallbacks.onDeviceConnecting(device);
            }
        }
        @Override
        public void onDeviceConnected(BluetoothDevice device) {
            if (mExternalManagerCallbacks != null) {
                mExternalManagerCallbacks.onDeviceConnected(device);
            }
        }
        @Override
        public void onDeviceDisconnecting(BluetoothDevice device) {
            if (mExternalManagerCallbacks != null) {
                mExternalManagerCallbacks.onDeviceDisconnecting(device);
            }
        }
        @Override
        public void onDeviceDisconnected(BluetoothDevice device) {
            if (mExternalManagerCallbacks != null) {
                mExternalManagerCallbacks.onDeviceDisconnected(device);
            }
        }
        @Override
        public void onLinklossOccur(BluetoothDevice device) {
            if (mExternalManagerCallbacks != null) {
                mExternalManagerCallbacks.onLinklossOccur(device);
            }
        }
        @Override
        public void onServicesDiscovered(BluetoothDevice device, boolean optionalServicesFound) {
            if (mExternalManagerCallbacks != null) {
                mExternalManagerCallbacks.onServicesDiscovered(device, optionalServicesFound);
            }
        }
        @Override
        public void onDeviceReady(BluetoothDevice device) {
            if (mExternalManagerCallbacks != null) {
                mExternalManagerCallbacks.onDeviceReady(device);
            }
        }
        @Override
        public boolean shouldEnableBatteryLevelNotifications(BluetoothDevice device) {
            return false;
        }
        @Override
        public void onBatteryValueReceived(BluetoothDevice device, int value) {
            if (mExternalManagerCallbacks != null) {
                mExternalManagerCallbacks.onBatteryValueReceived(device, value);
            }
        }
        @Override
        public void onBondingRequired(BluetoothDevice device) {
            if (mExternalManagerCallbacks != null) {
                mExternalManagerCallbacks.onBondingRequired(device);
            }
        }
        @Override
        public void onBonded(BluetoothDevice device) {
            if (mExternalManagerCallbacks != null) {
                mExternalManagerCallbacks.onBonded(device);
            }
        }
        @Override
        public void onDeviceNotSupported(BluetoothDevice device) {
            if (mExternalManagerCallbacks != null) {
                mExternalManagerCallbacks.onDeviceNotSupported(device);
            }
        }
    }
}
