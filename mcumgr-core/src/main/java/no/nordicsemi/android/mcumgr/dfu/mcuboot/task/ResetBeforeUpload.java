package no.nordicsemi.android.mcumgr.dfu.mcuboot.task;

class ResetBeforeUpload extends Reset {

	ResetBeforeUpload(final boolean noSwap) {
		super(noSwap);
	}

	@Override
	public int getPriority() {
		return PRIORITY_RESET_INITIAL;
	}
}
