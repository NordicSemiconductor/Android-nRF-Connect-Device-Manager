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

package io.runtime.mcumgr.def;

import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.util.ByteUtil;

public class McuMgrImageHeader {
	private static final int HEADER_LENGTH = 4 + 4 + 2 + 2 + 4 + 4 + 4;
	private static final int MAGIC = 0x96f3b83d;
	private int mMagic;
	private int mLoadAddr;
	private short mHdrSize;
	private short __mPad1;
	private int mImgSize;
	private int mFlags;
	private McuMgrImageVersion mVersion;
	private int __mPad2;

	private McuMgrImageHeader() {

	}

	public static McuMgrImageHeader fromBytes(byte[] b) throws McuMgrException {
		return fromBytes(b, 0);
	}

	public static McuMgrImageHeader fromBytes(byte[] b, int offset) throws McuMgrException {
		if (b.length - offset < sizeof()) {
			throw new IllegalStateException("The byte array is too short to be a McuMgrImageHeader");
		}

		McuMgrImageHeader header = new McuMgrImageHeader();
		header.mMagic = ByteUtil.unsignedByteArrayToInt(b, offset, 4);

		if (header.mMagic != MAGIC) {
			throw new McuMgrException("Wrong magic number: header=" + header.mMagic  +", magic=" + MAGIC);
		}

		header.mLoadAddr = ByteUtil.unsignedByteArrayToInt(b, 4 + offset, 4);
		header.mHdrSize = (short) ByteUtil.unsignedByteArrayToInt(b, 8 + offset, 2);
		header.mImgSize = ByteUtil.unsignedByteArrayToInt(b, 12 + offset, 4);
		header.mFlags = ByteUtil.unsignedByteArrayToInt(b, 16 + offset, 4);
		header.mVersion = McuMgrImageVersion.fromBytes(b, 20 + offset);

		return header;
	}

	public static int sizeof() {
		return McuMgrImageVersion.sizeof() + HEADER_LENGTH;
	}

	public int getMagic() {
		return mMagic;
	}

	public int getLoadAddr() {
		return mLoadAddr;
	}

	public short getHdrSize() {
		return mHdrSize;
	}

	public int getImgSize() {
		return mImgSize;
	}

	public int getFlags() {
		return mFlags;
	}

	public McuMgrImageVersion getVersion() {
		return mVersion;
	}

}
