/*
 *  Copyright (c) Intellinium SAS, 2014-present
 *  All Rights Reserved.
 *
 *  NOTICE:  All information contained herein is, and remains
 *  the property of Intellinium SAS and its suppliers,
 *  if any.  The intellectual and technical concepts contained
 *  herein are proprietary to Intellinium SAS
 *  and its suppliers and may be covered by French and Foreign Patents,
 *  patents in process, and are protected by trade secret or copyright law.
 *  Dissemination of this information or reproduction of this material
 *  is strictly forbidden unless prior written permission is obtained
 *  from Intellinium SAS.
 */

package io.runtime.mcumgr;

public enum McuMgrErrorCode {
	OK(0),
	UNKNOWN(1),
	NO_MEMORY(2),
	IN_VALUE(3),
	TIMEOUT(4),
	NO_ENTRY(5),
	BAD_STATE(6),
	TOO_LARGE(7),
	NOT_SUP(8),
	PERM_ERROR(256);

	private int mCode;

	McuMgrErrorCode(int code) {
		mCode = code;
	}

	public int value() {
		return mCode;
	}

	@Override
	public String toString() {
		return "NewtMgrError: " + super.toString() + "(" + mCode + ")";
	}

	public static McuMgrErrorCode valueOf(int error) {
		switch (error) {
			case 0:
				return OK;
			case 1:
				return UNKNOWN;
			case 2:
				return NO_MEMORY;
			case 3:
				return IN_VALUE;
			case 4:
				return TIMEOUT;
			case 5:
				return NO_ENTRY;
			case 6:
				return BAD_STATE;
			case 7:
				return TOO_LARGE;
			case 8:
				return NOT_SUP;
			case 256:
				return PERM_ERROR;
			default:
				return null;
		}
	}
}
