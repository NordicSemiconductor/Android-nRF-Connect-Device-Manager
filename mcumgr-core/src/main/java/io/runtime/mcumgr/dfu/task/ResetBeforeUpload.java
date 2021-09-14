package io.runtime.mcumgr.dfu.task;

class ResetBeforeUpload extends Reset {

	ResetBeforeUpload() {
	}

	@Override
	public int getPriority() {
		return PRIORITY_RESET_INITIAL;
	}
}
