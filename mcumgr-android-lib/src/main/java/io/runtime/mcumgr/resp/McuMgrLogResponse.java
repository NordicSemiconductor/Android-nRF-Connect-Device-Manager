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

package io.runtime.mcumgr.resp;

public class McuMgrLogResponse {
	public String name;
	public int type;
	public LogEntry[] entries;

	public static class LogEntry {
		public String msg;
		public long ts;
		public int level;
		public int index;
		public int module;
	}
}
