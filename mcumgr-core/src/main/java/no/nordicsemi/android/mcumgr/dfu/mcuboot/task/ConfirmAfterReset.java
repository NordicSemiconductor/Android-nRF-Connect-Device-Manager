package no.nordicsemi.android.mcumgr.dfu.mcuboot.task;

import org.jetbrains.annotations.NotNull;

class ConfirmAfterReset extends Confirm {

	ConfirmAfterReset(final byte @NotNull [] hash) {
		super(hash);
	}

	@Override
	public int getPriority() {
		return PRIORITY_CONFIRM_AFTER_RESET;
	}
}
