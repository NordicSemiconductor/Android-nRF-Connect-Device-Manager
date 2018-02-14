package io.runtime.mcumgr;

public interface McuMgrMtuCallback {
	void onMtuFetched(int mtu);

	void onMtuError();
}
