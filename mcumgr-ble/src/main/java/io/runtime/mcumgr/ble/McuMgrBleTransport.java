/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrHeader;
import io.runtime.mcumgr.McuMgrScheme;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.ble.callback.SmpDataCallback;
import io.runtime.mcumgr.ble.callback.SmpMerger;
import io.runtime.mcumgr.ble.callback.SmpResponse;
import io.runtime.mcumgr.exception.InsufficientMtuException;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.exception.McuMgrTimeoutException;
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.util.CBOR;
import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.Request;
import no.nordicsemi.android.ble.annotation.ConnectionPriority;
import no.nordicsemi.android.ble.callback.FailCallback;
import no.nordicsemi.android.ble.callback.MtuCallback;
import no.nordicsemi.android.ble.callback.SuccessCallback;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.ble.data.DataMerger;
import no.nordicsemi.android.ble.error.GattError;
import no.nordicsemi.android.ble.exception.BluetoothDisabledException;
import no.nordicsemi.android.ble.exception.DeviceDisconnectedException;
import no.nordicsemi.android.ble.exception.InvalidRequestException;
import no.nordicsemi.android.ble.exception.RequestFailedException;

/**
 * The McuMgrBleTransport is an implementation for the {@link McuMgrScheme#BLE} transport scheme.
 * This class extends {@link BleManager}, which handles the BLE state machine and owns the
 * {@link BluetoothGatt} object that executes BLE actions. If you wish to integrate McuManager an
 * existing BLE implementation, you may simply implement {@link McuMgrTransport} or use this class
 * to perform your BLE actions by calling {@link BleManager#enqueue(Request)}.
 */
@SuppressWarnings("unused")
public class McuMgrBleTransport extends BleManager implements McuMgrTransport {

    private static final Logger LOG = LoggerFactory.getLogger(McuMgrBleTransport.class);

    public final static UUID SMP_SERVICE_UUID =
            UUID.fromString("8D53DC1D-1DB7-4CD3-868B-8A527460AA84");
    private final static UUID SMP_CHAR_UUID =
            UUID.fromString("DA2E7828-FBCE-4E01-AE9E-261174997C48");

    /**
     * Simple Management Protocol service.
     */
    private BluetoothGattService mSmpService;

    /**
     * Simple Management Protocol characteristic.
     */
    private BluetoothGattCharacteristic mSmpCharacteristic;

    /**
     * The Bluetooth device for this transporter.
     */
    private final BluetoothDevice mDevice;

    /**
     * An instance of a merger used to merge SMP packets that are split into multiple BLE packets.
     */
    private final DataMerger mSMPMerger = new SmpMerger();

    /**
     * The maximum packet length supported by the target device.
     * This may be greater than MTU size.
     * For packets longer than this value an {@link InsufficientMtuException} will be thrown.
     * Packets longer than MTU, but shorter than this value will be split.
     * Splitting packets must be supported by SMP Server on the target device.
     */
    private int mMaxPacketLength;

    /**
     * Flag indicating should low-level logging be enabled. Default to false.
     * Call {@link #setLoggingEnabled(boolean)} to change.
     */
    private boolean mLoggingEnabled;

    /**
     * Construct a McuMgrBleTransport object.
     *
     * @param context the context used to connect to the device.
     * @param device  the device to connect to and communicate with.
     */
    public McuMgrBleTransport(@NonNull Context context, @NonNull BluetoothDevice device) {
        super(context);
        mDevice = device;
    }

    /**
     * Returns the device set in the constructor.
     *
     * @return The device to connect to and communicate with.
     */
    @NonNull
    @Override
    public BluetoothDevice getBluetoothDevice() {
        return mDevice;
    }

    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        return new McuMgrGattCallback();
    }

    /**
     * In order to send packets longer than MTU size, this library supports automatic splitting
     * of packets into at-most-MTU size chunks. This feature must be also supported by the target
     * device, as it must merge received chunks into a single packet, based on the length field
     * from the {@link io.runtime.mcumgr.McuMgrHeader}, included in the first chunk.
     * <p>
     * {@link io.runtime.mcumgr.managers.ImageManager} and
     * {@link io.runtime.mcumgr.managers.FsManager} will automatically split the file into multiple
     * SMP packets. This feature is about splitting SMP packet into chunks, not splitting data into
     * SMP packets.
     * <p>
     * This method sets the maximum packet length supported by the target device.
     * By default, this is be set to MTU - 3, which means that no splitting will be done.
     * <p>
     * Keep in mind, that before Android 5 requesting higher MTU was not supported. Setting the
     * maximum length to a greater value is required on those devices in order to upgrade
     * the firmware, send file or send any other SMP packet that is longer than 20 bytes.
     *
     * @param maxLength the maximum packet length.
     */
    public void setDeviceSidePacketMergingSupported(int maxLength) {
        mMaxPacketLength = maxLength;
    }

    //*******************************************************************************************
    // Logging
    //*******************************************************************************************

    /**
     * Allows to enable low-level logging. If enabled, all BLE events will be logged.
     *
     * @param enabled true to enable logging, false to disable (default).
     */
    public void setLoggingEnabled(boolean enabled) {
        mLoggingEnabled = enabled;
    }

    @Override
    public void log(int priority, @NonNull String message) {
        if (mLoggingEnabled) {
            switch (priority) {
                case Log.DEBUG:
                    LOG.debug(message);
                    break;
                case Log.INFO:
                    LOG.info(message);
                    break;
                case Log.WARN:
                    LOG.warn(message);
                    break;
                case Log.ERROR:
                case Log.ASSERT:
                    LOG.error(message);
                    break;
                case Log.VERBOSE:
                default:
                    LOG.trace(message);
                    break;
            }
        }
    }

    //*******************************************************************************************
    // Mcu Manager Transport
    //*******************************************************************************************

    @NonNull
    @Override
    public McuMgrScheme getScheme() {
        return McuMgrScheme.BLE;
    }

    @NonNull
    @Override
    public <T extends McuMgrResponse> T send(@NonNull final byte[] payload,
                                             @NonNull final Class<T> responseType)
            throws McuMgrException {
        // If device is not connected, connect
        final boolean wasConnected = isConnected();
        try {
            // Await will wait until the device is ready (that is initialization is complete)
            connect(mDevice)
                    .retry(3, 100)
                    .timeout(25 * 1000)
                    .await();
            if (!wasConnected) {
                notifyConnected();
            }
        } catch (RequestFailedException e) {
            switch (e.getStatus()) {
                case FailCallback.REASON_DEVICE_NOT_SUPPORTED:
                    throw new McuMgrException("Device does not support SMP Service");
                case FailCallback.REASON_REQUEST_FAILED:
                    // This could be thrown only if the manager was requested to connect for
                    // a second time and to a different device than the one that's already
                    // connected. This may not happen here.
                    throw new McuMgrException("Other device already connected");
                case FailCallback.REASON_TIMEOUT:
                    // Called after receiving error 133 after 30 seconds.
                    throw new McuMgrTimeoutException();
                default:
                    // Other errors are currently never thrown for the connect request.
                    throw new McuMgrException("Unknown error");
            }
        } catch (InterruptedException e) {
            // On timeout, fail the request
            throw new McuMgrException("Connection routine timed out.");
        } catch (DeviceDisconnectedException e) {
            // When connection failed, fail the request
            throw new McuMgrException("Device has disconnected");
        } catch (BluetoothDisabledException e) {
            // When Bluetooth was disabled, fail the request
            throw new McuMgrException("Bluetooth adapter disabled");
        } catch (InvalidRequestException e) {
            // Ignore. This exception won't be thrown
            throw new RuntimeException("Invalid request");
        }

        // Ensure the MTU is sufficient.
        if (mMaxPacketLength < payload.length) {
            throw new InsufficientMtuException(payload.length, mMaxPacketLength);
        }

        // Send the request and wait for a notification in a synchronous way
        try {
            if (mLoggingEnabled) {
                try {
                    log(Log.VERBOSE, "Sending "
                            + McuMgrHeader.fromBytes(payload).toString() + " CBOR "
                            + CBOR.toString(payload, McuMgrHeader.HEADER_LENGTH));
                } catch (Exception e) {
                    // Ignore
                }
            }
            final SmpResponse<T> smpResponse = waitForNotification(mSmpCharacteristic)
                    .merge(mSMPMerger)
                    .trigger(writeCharacteristic(mSmpCharacteristic, payload).split())
                    .timeout(30000)
                    .await(new SmpResponse<>(responseType));
            if (smpResponse.isValid()) {
                if (mLoggingEnabled) {
                    try {
                        byte[] response = smpResponse.getRawData().getValue();
                        //noinspection ConstantConditions
                        log(Log.INFO, "Received "
                                + McuMgrHeader.fromBytes(response).toString() + " CBOR "
                                + CBOR.toString(response, McuMgrHeader.HEADER_LENGTH));
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                //noinspection ConstantConditions
                return smpResponse.getResponse();
            } else {
                throw new McuMgrException("Error building " +
                        "McuMgrResponse from response data: " + smpResponse.getRawData());
            }
        } catch (RequestFailedException e) {
            throw new McuMgrException(GattError.parse(e.getStatus()));
        } catch (InterruptedException e) {
            // Device must have disconnected moment before the request was made
            throw new McuMgrException("Request timed out");
        } catch (DeviceDisconnectedException e) {
            // When connection failed, fail the request
            throw new McuMgrException("Device has disconnected");
        } catch (BluetoothDisabledException e) {
            // When Bluetooth was disabled, fail the request
            throw new McuMgrException("Bluetooth adapter disabled");
        } catch (InvalidRequestException e) {
            // Ignore. This exception won't be thrown
            throw new RuntimeException("Invalid request");
        }
    }

    @Override
    public <T extends McuMgrResponse> void send(@NonNull final byte[] payload,
                                                @NonNull final Class<T> responseType,
                                                @NonNull final McuMgrCallback<T> callback) {
        // If device is not connected, connect.
        // If the device was already connected, the completion callback will be called immediately.
        final boolean wasConnected = isConnected();
        connect(mDevice).done(new SuccessCallback() {
            @Override
            public void onRequestCompleted(@NonNull final BluetoothDevice device) {
                if (!wasConnected) {
                    notifyConnected();
                }

                // Ensure the MTU is sufficient. Packets longer than MTU, but shorter
                // then few MTU lengths can be split automatically.
                if (mMaxPacketLength < payload.length) {
                    callback.onError(new InsufficientMtuException(payload.length, mMaxPacketLength));
                    return;
                }

                if (mLoggingEnabled) {
                    try {
                        log(Log.VERBOSE, "Sending "
                                + McuMgrHeader.fromBytes(payload).toString() + " CBOR "
                                + CBOR.toString(payload, McuMgrHeader.HEADER_LENGTH));
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                waitForNotification(mSmpCharacteristic)
                        .merge(mSMPMerger)
                        .with(new SmpDataCallback<T>(responseType) {
                            @Override
                            public void onDataReceived(@NonNull BluetoothDevice device,
                                                       @NonNull Data data) {
                                if (mLoggingEnabled) {
                                    try {
                                        byte[] response = data.getValue();
                                        //noinspection ConstantConditions
                                        log(Log.INFO, "Received "
                                                + McuMgrHeader.fromBytes(response).toString() + " CBOR "
                                                + CBOR.toString(response, McuMgrHeader.HEADER_LENGTH));
                                    } catch (Exception e) {
                                        // Ignore
                                    }
                                }
                                super.onDataReceived(device, data);
                            }

                            @Override
                            public void onResponseReceived(@NonNull BluetoothDevice device,
                                                           @NonNull T response) {
                                if (response.isSuccess()) {
                                    callback.onResponse(response);
                                } else {
                                    callback.onError(new McuMgrErrorException(response));
                                }
                            }

                            @Override
                            public void onInvalidDataReceived(@NonNull BluetoothDevice device,
                                                              @NonNull Data data) {
                                callback.onError(new McuMgrException("Error building " +
                                        "McuMgrResponse from response data: " + data));
                            }
                        })
                        .trigger(writeCharacteristic(mSmpCharacteristic, payload).split())
                        .fail(new FailCallback() {
                            @Override
                            public void onRequestFailed(@NonNull BluetoothDevice device,
                                                        int status) {
                                switch (status) {
                                    case REASON_TIMEOUT:
                                        callback.onError(new McuMgrException("Request timed out"));
                                        break;
                                    case REASON_DEVICE_DISCONNECTED:
                                        callback.onError(new McuMgrException("Device has disconnected"));
                                        break;
                                    case REASON_BLUETOOTH_DISABLED:
                                        callback.onError(new McuMgrException("Bluetooth adapter disabled"));
                                        break;
                                    default:
                                        callback.onError(new McuMgrException(GattError.parse(status)));
                                        break;
                                }
                            }
                        })
                        .timeout(30000)
                        .enqueue();
            }
        }).fail(new FailCallback() {
            @Override
            public void onRequestFailed(@NonNull final BluetoothDevice device, final int status) {
                switch (status) {
                    case REASON_DEVICE_DISCONNECTED:
                        callback.onError(new McuMgrException("Device has disconnected"));
                        break;
                    case REASON_DEVICE_NOT_SUPPORTED:
                        callback.onError(new McuMgrException("Device does not support SMP Service"));
                        break;
                    case REASON_REQUEST_FAILED:
                        // This could be thrown only if the manager was requested to connect for
                        // a second time and to a different device than the one that's already
                        // connected. This may not happen here.
                        callback.onError(new McuMgrException("Other device already connected"));
                        break;
                    case REASON_TIMEOUT:
                        // Called after receiving error 133 after 30 seconds.
                        callback.onError(new McuMgrTimeoutException());
                        break;
                    case REASON_BLUETOOTH_DISABLED:
                        callback.onError(new McuMgrException("Bluetooth adapter disabled"));
                        break;
                    default:
                        callback.onError(new McuMgrException(GattError.parseConnectionError(status)));
                        break;
                }
            }
        })
        .retry(3, 100)
        .enqueue();
    }

    @Override
    public void connect(@Nullable final ConnectionCallback callback) {
        if (isConnected()) {
            if (callback != null) {
                callback.onConnected();
            }
            return;
        }
        connect(mDevice)
                .retry(3, 100)
                .done(new SuccessCallback() {
                    @Override
                    public void onRequestCompleted(@NonNull BluetoothDevice device) {
                        notifyConnected();
                        if (callback == null) {
                            return;
                        }
                        callback.onConnected();
                    }
                })
                .fail(new FailCallback() {
                    @Override
                    public void onRequestFailed(@NonNull BluetoothDevice device, int status) {
                        if (callback == null) {
                            return;
                        }
                        switch (status) {
                            case REASON_DEVICE_DISCONNECTED:
                                callback.onError(new McuMgrException("Device has disconnected"));
                                break;
                            case REASON_DEVICE_NOT_SUPPORTED:
                                callback.onError(new McuMgrException("Device does not support SMP Service"));
                                break;
                            case REASON_REQUEST_FAILED:
                                // This could be thrown only if the manager was requested to connect for
                                // a second time and to a different device than the one that's already
                                // connected. This may not happen here.
                                callback.onError(new McuMgrException("Other device already connected"));
                                break;
                            case REASON_BLUETOOTH_DISABLED:
                                callback.onError(new McuMgrException("Bluetooth adapter disabled"));
                                break;
                            default:
                                callback.onError(new McuMgrException(GattError.parseConnectionError(status)));
                                break;
                        }
                    }
                })
                .enqueue();
    }

    @Override
    public void release() {
        cancelQueue();
        disconnect().enqueue();
    }

    /**
     * Requests the given connection priority. On Android, the connection priority is the
     * equivalent of connection parameters. Acceptable values are:
     * <ol>
     * <li>{@link BluetoothGatt#CONNECTION_PRIORITY_HIGH}
     * - Interval: 11.25 -15 ms, latency: 0, supervision timeout: 20 sec,</li>
     * <li>{@link BluetoothGatt#CONNECTION_PRIORITY_BALANCED}
     * - Interval: 30 - 50 ms, latency: 0, supervision timeout: 20 sec,</li>
     * <li>{@link BluetoothGatt#CONNECTION_PRIORITY_LOW_POWER}
     * - Interval: 100 - 125 ms, latency: 2, supervision timeout: 20 sec.</li>
     * </ol>
     * Calling this method with priority {@link BluetoothGatt#CONNECTION_PRIORITY_HIGH} may
     * improve file transfer speed.
     * <p>
     * Similarly to {@link #send(byte[], Class)}, this method will connect automatically
     * to the device if not connected.
     *
     * @param priority one of: {@link BluetoothGatt#CONNECTION_PRIORITY_HIGH},
     *                 {@link BluetoothGatt#CONNECTION_PRIORITY_BALANCED},
     *                 {@link BluetoothGatt#CONNECTION_PRIORITY_LOW_POWER}.
     */
    public void requestConnPriority(@ConnectionPriority final int priority) {
        connect(mDevice).done(new SuccessCallback() {
            @Override
            public void onRequestCompleted(@NonNull BluetoothDevice device) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    McuMgrBleTransport.super.requestConnectionPriority(priority).enqueue();
                } // else ignore... :(
            }
        })
        .retry(3, 100)
        .enqueue();
    }

    //*******************************************************************************************
    // Ble Manager Callbacks
    //*******************************************************************************************

    private class McuMgrGattCallback extends BleManagerGattCallback {

        // Determines whether the device supports the SMP Service
        @Override
        protected boolean isRequiredServiceSupported(@NonNull BluetoothGatt gatt) {
            mSmpService = gatt.getService(SMP_SERVICE_UUID);
            if (mSmpService == null) {
                LOG.error("Device does not support SMP service");
                return false;
            }
            mSmpCharacteristic = mSmpService.getCharacteristic(SMP_CHAR_UUID);
            if (mSmpCharacteristic == null) {
                LOG.error("Device does not support SMP characteristic");
                return false;
            } else {
                final int rxProperties = mSmpCharacteristic.getProperties();
                boolean write = (rxProperties &
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0;
                boolean notify = (rxProperties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0;
                if (!write || !notify) {
                    LOG.error("SMP characteristic does not support either write ({}) or notify ({})", write, notify);
                    return false;
                }
            }
            return true;
        }

        // Called once the connection has been established and services discovered. This method
        // adds a queue of requests necessary to set up the SMP service to begin writing
        // commands and receiving responses. Once these actions have completed onDeviceReady is
        // called.
        @Override
        protected void initialize() {
            requestMtu(515)
                    .with(new MtuCallback() {
                        @Override
                        public void onMtuChanged(@NonNull final BluetoothDevice device, final int mtu) {
                            mMaxPacketLength = Math.max(mtu - 3, mMaxPacketLength);
                        }
                    })
                    .fail(new FailCallback() {
                        @Override
                        public void onRequestFailed(@NonNull final BluetoothDevice device, final int status) {
                            mMaxPacketLength = Math.max(getMtu() - 3, mMaxPacketLength);
                        }
                    }).enqueue();
            enableNotifications(mSmpCharacteristic).enqueue();
        }

        // Called when the device has disconnected. This method nulls the services and
        // characteristic variables.
        @Override
        protected void onDeviceDisconnected() {
            mSmpService = null;
            mSmpCharacteristic = null;

            runOnCallbackThread(new Runnable() {
                @Override
                public void run() {
                    notifyDisconnected();
                }
            });
        }
    }

    //*******************************************************************************************
    // Manager Connection Observers
    //*******************************************************************************************

    private final List<ConnectionObserver> mConnectionObservers = new LinkedList<>();

    @Override
    public synchronized void addObserver(@NonNull final ConnectionObserver observer) {
        mConnectionObservers.add(observer);
    }

    @Override
    public synchronized void removeObserver(@NonNull final ConnectionObserver observer) {
        mConnectionObservers.remove(observer);
    }

    private synchronized void notifyConnected() {
        for (ConnectionObserver o : mConnectionObservers) {
            o.onConnected();
        }
    }

    private synchronized void notifyDisconnected() {
        for (ConnectionObserver o : mConnectionObservers) {
            o.onDisconnected();
        }
    }
}
