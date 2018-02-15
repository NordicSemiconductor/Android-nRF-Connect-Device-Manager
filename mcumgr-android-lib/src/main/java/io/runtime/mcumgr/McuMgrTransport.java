/***************************************************************************
 * Copyright (c) Intellinium SAS, 2014-present
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Intellinium SAS and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Intellinium SAS
 * and its suppliers and may be covered by French and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Intellinium SAS.
 ***************************************************************************/
/* TODO: add runtime copyright */

package io.runtime.mcumgr;

import io.runtime.mcumgr.exception.McuMgrException;

public abstract class McuMgrTransport {
	private McuManager.Scheme mScheme;

	protected McuMgrTransport(McuManager.Scheme scheme) {
		mScheme = scheme;
	}

	public McuManager.Scheme getScheme() {
		return mScheme;
	}

	public abstract McuMgrResponse send(byte[] payload) throws McuMgrException;

	public abstract void send(byte[] payload, McuMgrCallback callback);

	public abstract void init(McuMgrInitCallback cb);
}
