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
import io.runtime.mcumgr.resp.McuMgrResponse;

public abstract class McuMgrTransport {
	private McuMgrScheme mScheme;

	protected McuMgrTransport(McuMgrScheme scheme) {
		mScheme = scheme;
	}

	McuMgrScheme getScheme() {
		return mScheme;
	}

	public abstract void init(McuMgrInitCallback cb);

	public abstract <T extends McuMgrResponse> T send(byte[] payload, Class<T> responseType) throws McuMgrException;

	public abstract <T extends McuMgrResponse> void send(byte[] payload, Class<T> responseType,
														 McuMgrCallback<T> callback);
}
