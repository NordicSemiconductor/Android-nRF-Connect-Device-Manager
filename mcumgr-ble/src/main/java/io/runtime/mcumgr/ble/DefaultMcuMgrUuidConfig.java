package io.runtime.mcumgr.ble;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class DefaultMcuMgrUuidConfig implements UuidConfig {
	public final static UUID SMP_SERVICE_UUID =
			UUID.fromString("8D53DC1D-1DB7-4CD3-868B-8A527460AA84");
	public final static UUID SMP_CHAR_UUID =
			UUID.fromString("DA2E7828-FBCE-4E01-AE9E-261174997C48");

	@NotNull
	@Override
	public UUID getServiceUuid() {
		return SMP_SERVICE_UUID;
	}

	@NotNull
	@Override
	public UUID getCharacteristicUuid() {
		return SMP_CHAR_UUID;
	}
}
