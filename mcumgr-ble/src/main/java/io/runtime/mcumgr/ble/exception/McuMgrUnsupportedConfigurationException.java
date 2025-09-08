package io.runtime.mcumgr.ble.exception;

import io.runtime.mcumgr.exception.McuMgrException;

/**
 * This exception is thrown when the Android device fails to connect to the peripheral
 * due to its internal issues. Most probably it cannot respond properly to PHY LE 2M
 * update procedure, causing the remote device to terminate the connection.
 */
public class McuMgrUnsupportedConfigurationException extends McuMgrException {
}
