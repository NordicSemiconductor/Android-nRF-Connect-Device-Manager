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

import java.util.Map;

public class McuMgrTaskStatResponse extends McuMgrSimpleResponse {
	public Map<String, TaskStat> tasks;

	public static class TaskStat {
		public int prio;
		public int tid;
		public int state;
		public int stkuse;
		public int stksiz;
		public int cswcnt;
		public int runtime;
		public int last_checkin;
		public int next_checkin;
	}
}
