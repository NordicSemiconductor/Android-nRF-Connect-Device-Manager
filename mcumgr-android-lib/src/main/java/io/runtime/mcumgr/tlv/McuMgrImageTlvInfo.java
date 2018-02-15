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

package io.runtime.mcumgr.tlv;

import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.util.ByteUtil;

public class McuMgrImageTlvInfo {
	public static final int MAGIC = 0x6907;

	private short mMagic;
	private short mTotal;

	private McuMgrImageTlvInfo() {

	}

	public static McuMgrImageTlvInfo fromBytes(byte[] b) throws McuMgrException {
		return fromBytes(b, 0);
	}

	public static McuMgrImageTlvInfo fromBytes(byte[] b, int offset) throws McuMgrException {
		if (b.length - offset < sizeof())
			throw new McuMgrException("The byte array is too short to be a McuMgrImageTlvInfo: " +
					"length= " + b.length + ", offset= " + offset + ", min size= " + sizeof());

		McuMgrImageTlvInfo info = new McuMgrImageTlvInfo();
		info.mMagic = (short) ByteUtil.unsignedByteArrayToInt(b, offset, 2);
		info.mTotal = (short) ByteUtil.unsignedByteArrayToInt(b, offset + 2, 2);

		if (info.mMagic != MAGIC) {
			throw new McuMgrException("Wrong magic number");
		}

		return info;
	}

	public static int sizeof() {
		return 4;
	}

	public short getMagic() {
		return mMagic;
	}

	public short getTotal() {
		return mTotal;
	}
}
