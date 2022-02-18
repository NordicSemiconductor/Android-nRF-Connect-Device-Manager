package io.runtime.mcumgr.ble;

import java.util.UUID;

import androidx.annotation.NonNull;

/**
 * The UUID Config allows providing a custom set of UUIDs for the McuMgr BLE Transport.
 */
public interface UuidConfig {

	/** The SMP service UUID. */
	@NonNull UUID getServiceUuid();

	/** The SMP characteristic UUID. */
	@NonNull UUID getCharacteristicUuid();
}
