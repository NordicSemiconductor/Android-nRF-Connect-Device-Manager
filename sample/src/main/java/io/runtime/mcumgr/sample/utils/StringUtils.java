/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.utils;

public class StringUtils {
	private final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

	public static String toHex(final byte[] data) {
		if (data == null || data.length == 0)
			return "";

		final char[] out = new char[data.length * 2];
		for (int j = 0; j < data.length; j++) {
			int v = data[j] & 0xFF;
			out[j * 2] = HEX_ARRAY[v >>> 4];
			out[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(out);
	}
}
