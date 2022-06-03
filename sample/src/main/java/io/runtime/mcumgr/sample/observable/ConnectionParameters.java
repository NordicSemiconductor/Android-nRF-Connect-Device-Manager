package io.runtime.mcumgr.sample.observable;

import no.nordicsemi.android.ble.annotation.PhyValue;

public class ConnectionParameters {
	private final int interval;
	private final int latency;
	private final int timeout;
	private final int mtu;
	private final int bufferSize;
	private final int txPhy, rxPhy;

	public ConnectionParameters(
			final int interval, final int latency, final int timeout,
			final int mtu, final int bufferSize,
			final int txPhy, final int rxPhy) {
		this.interval = interval;
		this.latency = latency;
		this.timeout = timeout;
		this.mtu = mtu;
		this.bufferSize = bufferSize;
		this.txPhy = txPhy;
		this.rxPhy = rxPhy;
	}

	/**
	 * Connection interval used on this connection, 1.25ms unit.
	 * Valid range is from 6 (7.5ms) to 3200 (4000ms).
	 *
	 * @return The connection interval in 1.25ms units.
	 */
	public int getInterval() {
		return interval;
	}

	/**
	 * Returns the connection interval in milliseconds.
	 * Valid range is from 7.5ms to 4000ms.
	 *
	 * @return The connection interval in milliseconds.
	 */
	public float getIntervalInMs() {
		return (float) interval * 1.25f; // ms
	}

	/**
	 * Returns the slave latency for the connection in number of connection events.
	 * Valid range is from 0 to 499.
	 *
	 * @return The slave latency value.
	 */
	public int getLatency() {
		return latency;
	}

	/**
	 * Returns the supervision timeout for this connection, in 10ms unit.
	 * Valid range is from 10 (100 ms = 0.1s) to 3200 (32s).
	 *
	 * @return Supervision timeout for this connection, in 10ms unit.
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * Returns the supervision timeout for this connection, in milliseconds.
	 * Valid range is from 100 ms 32s.
	 *
	 * @return Supervision timeout for this connection, in 10ms unit.
	 */
	public long getTimeoutInMs() {
		return timeout * 10L; // ms
	}

	/**
	 * Returns the current MTU. The maximum number of bytes that can be sent in a single GATT
	 * operation is MTU-3, as 3 bytes are used for handle number and operation.
	 *
	 * @return The MTU.
	 */
	public int getMtu() {
		return mtu;
	}

	/**
	 * Returns the current McuMgr buffer size used in this connection.
	 * The buffer allows to send longer SMP packets than MTU using the SAR mechanism (Segmentation
	 * and Reassembly).
	 *
	 * @return The buffers size in bytes. If SAR is not supported (pre-NCS 2.0 devices) this is
	 *         equal to MTU.
	 */
	public int getBufferSize() {
		return bufferSize;
	}

	/**
	 * Returns the current TX PHY.
	 *
	 * @return The current TX PHY.
	 */
	@PhyValue
	public int getTxPhy() {
		return txPhy;
	}

	/**
	 * Returns the current RX PHY.
	 *
	 * @return The current RX PHY.
	 */
	@PhyValue
	public int getRxPhy() {
		return rxPhy;
	}
}
